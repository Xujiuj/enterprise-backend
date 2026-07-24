package org.dromara.common.mybatis.handler;

import cn.dev33.satoken.exception.NotLoginException;
import cn.hutool.http.HttpStatus;
import com.baomidou.dynamic.datasource.exception.CannotFindDataSourceException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.domain.R;
import org.mybatis.spring.MyBatisSystemException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Mybatis异常处理器
 *
 * @author Lion Li
 */
@Slf4j
@RestControllerAdvice
public class MybatisExceptionHandler {

    /**
     * 主键或UNIQUE索引，数据重复异常
     */
    @ExceptionHandler(DuplicateKeyException.class)
    public R<Void> handleDuplicateKeyException(DuplicateKeyException e, HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        log.error("请求地址'{}',数据库中已存在记录'{}'", requestURI, e.getMessage());
        return R.fail(HttpStatus.HTTP_CONFLICT, "数据库中已存在该记录，请联系管理员确认");
    }

    /**
     * Mybatis系统异常 通用处理
     */
    @ExceptionHandler(MyBatisSystemException.class)
    public R<Void> handleCannotFindDataSourceException(MyBatisSystemException e, HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        Throwable root = getRootCause(e);
        if (root instanceof NotLoginException notLoginException) {
            String message = getNotLoginMessage(notLoginException);
            log.error("请求地址'{}',认证失败'{}',类型'{}'", requestURI, root.getMessage(), notLoginException.getType());
            return R.fail(HttpStatus.HTTP_UNAUTHORIZED, message);
        }
        if (root instanceof CannotFindDataSourceException) {
            log.error("请求地址'{}', 未找到数据源", requestURI);
            return R.fail(HttpStatus.HTTP_INTERNAL_ERROR, "未找到数据源，请联系管理员确认");
        }
        log.error("请求地址'{}', Mybatis系统异常", requestURI, e);
        return R.fail(HttpStatus.HTTP_INTERNAL_ERROR, e.getMessage());
    }

    /**
     * 获取异常的根因（递归查找）
     *
     * @param e 当前异常
     * @return 根因异常（最底层的 cause）
     * <p>
     * 逻辑说明：
     * 1. 如果 e 没有 cause，说明 e 本身就是根因，直接返回
     * 2. 如果 e 的 cause 和自身相同（防止循环引用），也返回 e
     * 3. 否则递归调用，继续向下寻找最底层的 cause
     */
    public static Throwable getRootCause(Throwable e) {
        Throwable cause = e.getCause();
        if (cause == null || cause == e) {
            return e;
        }
        return getRootCause(cause);
    }

    private String getNotLoginMessage(NotLoginException e) {
        String type = e.getType();
        if (NotLoginException.BE_REPLACED.equals(type)) {
            return "当前账号已在其他设备登录，本次登录状态已失效，请重新登录。";
        }
        if (NotLoginException.KICK_OUT.equals(type)) {
            return "当前账号已被管理员强制下线，请重新登录。";
        }
        if (NotLoginException.TOKEN_TIMEOUT.equals(type)) {
            return "登录已超过 1 小时，请重新登录。";
        }
        if (NotLoginException.INVALID_TOKEN.equals(type) || NotLoginException.NOT_TOKEN.equals(type)) {
            return "登录状态已失效，请重新登录。";
        }
        return "登录状态异常，请重新登录。";
    }

    /**
     * 在异常链中查找指定类型的异常
     *
     * @param e     当前异常
     * @param clazz 目标异常类
     * @return 找到的指定类型异常，如果没有找到返回 null
     */
    public static Throwable findCause(Throwable e, Class<? extends Throwable> clazz) {
        Throwable t = e;
        while (t != null && t != t.getCause()) {
            if (clazz.isInstance(t)) {
                return t;
            }
            t = t.getCause();
        }
        return null;
    }

}
