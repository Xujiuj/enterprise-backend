package org.dromara.common.web.core;

import org.dromara.common.core.domain.R;
import org.dromara.common.core.utils.StringUtils;

/**
 * web层通用数据处理
 *
 * @author Lion Li
 */
public class BaseController {

    private static final String OPERATION_FAILED_MESSAGE = "操作失败：未更新到任何数据，请确认记录是否存在或当前状态是否允许操作";

    /**
     * 响应返回结果
     *
     * @param rows 影响行数
     * @return 操作结果
     */
    protected R<Void> toAjax(int rows) {
        return rows > 0 ? R.ok() : R.fail(OPERATION_FAILED_MESSAGE);
    }

    /**
     * 响应返回结果
     *
     * @param result 结果
     * @return 操作结果
     */
    protected R<Void> toAjax(boolean result) {
        return result ? R.ok() : R.fail(OPERATION_FAILED_MESSAGE);
    }

    /**
     * 页面跳转
     */
    public String redirect(String url) {
        return StringUtils.format("redirect:{}", url);
    }

}
