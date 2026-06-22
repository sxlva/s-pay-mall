package cn.fcr.domain.mall.model.exception;

/**
 * 商品仍有关联订单异常
 * <p>
 * 在删除商品时，若该商品下仍存在关联订单则抛出此异常。
 * 替代 GlobalExceptionHandler 中解析 SQL 错误消息的反模式。
 *
 * @author 傅崇睿
 */
public class ProductHasOrdersException extends RuntimeException {

    public ProductHasOrdersException(String message) {
        super(message);
    }
}
