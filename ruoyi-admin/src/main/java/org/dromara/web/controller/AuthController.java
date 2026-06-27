package org.dromara.web.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import cn.hutool.core.util.ObjectUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.constant.SystemConstants;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.domain.model.LoginBody;
import org.dromara.common.core.utils.*;
import org.dromara.common.encrypt.annotation.ApiEncrypt;
import org.dromara.common.json.utils.JsonUtils;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.common.sse.dto.SseMessageDto;
import org.dromara.common.sse.utils.SseMessageUtils;
import org.dromara.system.domain.vo.SysClientVo;
import org.dromara.system.service.ISysClientService;
import org.dromara.web.domain.vo.LoginVo;
import org.dromara.web.service.IAuthStrategy;
import org.dromara.web.service.SysLoginService;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 认证
 *
 * @author Lion Li
 */
@Slf4j
@SaIgnore
@RequiredArgsConstructor
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final SysLoginService loginService;
    private final ISysClientService clientService;
    private final ScheduledExecutorService scheduledExecutorService;


    /**
     * 登录方法
     *
     * @param body 登录信息
     * @return 结果
     */
    @ApiEncrypt
    @PostMapping("/login")
    public R<LoginVo> login(@RequestBody String body) {
        LoginBody loginBody = JsonUtils.parseObject(body, LoginBody.class);
        ValidatorUtils.validate(loginBody);
        // 授权类型和客户端id
        String clientId = loginBody.getClientId();
        String grantType = loginBody.getGrantType();
        SysClientVo client = clientService.queryByClientId(clientId);
        // 查询不到 client 或 client 内不包含 grantType
        if (ObjectUtil.isNull(client) || !StringUtils.contains(client.getGrantType(), grantType)) {
            log.info("客户端id: {} 认证类型：{} 异常!.", clientId, grantType);
            return R.fail(MessageUtils.message("auth.grant.type.error"));
        } else if (!SystemConstants.NORMAL.equals(client.getStatus())) {
            return R.fail(MessageUtils.message("auth.grant.type.blocked"));
        }
        // 校验租户
        loginService.checkTenant(loginBody.getTenantId());
        // 登录
        LoginVo loginVo = IAuthStrategy.login(body, client, grantType);

        Long userId = LoginHelper.getUserId();
        scheduledExecutorService.schedule(() -> {
            SseMessageDto dto = new SseMessageDto();
            dto.setMessage(DateUtils.getTodayHour(new Date()) + "好，欢迎登录企业碳数据管理平台");
            dto.setUserIds(List.of(userId));
            SseMessageUtils.publishMessage(dto);
        }, 5, TimeUnit.SECONDS);
        return R.ok(loginVo);
    }

    /**
     * 退出登录
     */
    @PostMapping("/logout")
    public R<Void> logout() {
        loginService.logout();
        return R.ok("退出成功");
    }

}
