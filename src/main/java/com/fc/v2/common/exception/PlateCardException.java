package com.fc.v2.common.exception;

/**
 * 门楼牌号段卡业务校验异常。
 * 校验的本事收在服务层：卡落不下去时，连错在哪一格一起说清。
 * 由全局异常解析器以接口回话（AjaxResult.error）原样回给门脸，页面不得自行涂改。
 *
 * @author fuce
 * @date 2026-09-27
 */
public class PlateCardException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    /** 出错的格口（字段名），如 qty / billNo / siteId */
    private final String field;

    public PlateCardException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
