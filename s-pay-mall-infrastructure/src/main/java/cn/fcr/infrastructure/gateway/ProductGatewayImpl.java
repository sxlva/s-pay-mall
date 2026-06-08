package cn.fcr.infrastructure.gateway;

import cn.fcr.domain.order.gateway.IProductGateway;
import cn.fcr.domain.order.model.entity.ProductEntity;
import cn.fcr.infrastructure.dao.IProductDao;
import cn.fcr.infrastructure.gateway.dto.ProductDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author 傅崇睿
 * @description 产品网关实现
 */
@Slf4j
@Component
public class ProductGatewayImpl implements IProductGateway {

    private final ProductRPC productRPC;

    @Resource
    private IProductDao productDao;

    public ProductGatewayImpl(ProductRPC productRPC) {
        this.productRPC = productRPC;
    }

    @Override
    public ProductEntity queryProductByProductId(String productId) {
        ProductDTO productDTO = productRPC.queryProductByProductId(productId);
        return ProductEntity.builder()
                .productId(productDTO.getProductId())
                .productName(productDTO.getProductName())
                .productDesc(productDTO.getProductDesc())
                .price(productDTO.getPrice())
                .build();
    }

    @Override
    public void restoreStock(String productId, Integer quantity) {
        try {
            Long id = Long.parseLong(productId);
            productDao.increaseStock(id, quantity);
            log.info("库存恢复成功: productId={}, quantity={}", productId, quantity);
        } catch (NumberFormatException e) {
            log.error("商品ID格式错误: productId={}", productId, e);
            throw new RuntimeException("商品ID格式错误: " + productId, e);
        }
    }
}