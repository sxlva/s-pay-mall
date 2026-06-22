package cn.fcr.domain.mall.model.exception;

/**
 * 分类下仍有关联商品异常
 * <p>
 * 在删除分类时，若该分类下仍存在关联商品则抛出此异常。
 * 替代 GlobalExceptionHandler 中解析 SQL 错误消息的反模式。
 *
 * @author 傅崇睿
 */
public class CategoryHasProductsException extends RuntimeException {

    public CategoryHasProductsException(String message) {
        super(message);
    }
}
