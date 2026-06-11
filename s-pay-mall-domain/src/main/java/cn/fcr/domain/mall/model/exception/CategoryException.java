package cn.fcr.domain.mall.model.exception;

/**
 * 【DDD 领域异常】分类业务异常
 * 用于表示分类相关的业务规则违反
 *
 * @author 傅崇睿
 */
public class CategoryException extends RuntimeException {

    /**
     * 错误码枚举
     */
    public enum ErrorCode {
        /**
         * 分类不存在
         */
        CATEGORY_NOT_FOUND("分类不存在"),

        /**
         * 分类不活跃
         */
        CATEGORY_INACTIVE("分类状态不活跃"),

        /**
         * 分类下存在商品，无法删除
         */
        CATEGORY_HAS_PRODUCTS("该分类下仍有关联商品，无法删除"),

        /**
         * 分类下存在子分类，无法删除
         */
        CATEGORY_HAS_CHILDREN("该分类下存在子分类，无法删除"),

        /**
         * 无效的父分类ID
         */
        INVALID_PARENT_ID("无效的父分类ID"),

        /**
         * 分类名称为空
         */
        EMPTY_CATEGORY_NAME("分类名称不能为空"),

        /**
         * 分类名称过长
         */
        CATEGORY_NAME_TOO_LONG("分类名称长度不能超过100个字符"),

        /**
         * 无法创建子分类（达到层级限制）
         */
        MAX_LEVEL_EXCEEDED("分类层级超过最大限制");

        private final String message;

        ErrorCode(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }

    private final ErrorCode errorCode;

    public CategoryException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public CategoryException(ErrorCode errorCode, String detail) {
        super(errorCode.getMessage() + (detail != null ? ": " + detail : ""));
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}