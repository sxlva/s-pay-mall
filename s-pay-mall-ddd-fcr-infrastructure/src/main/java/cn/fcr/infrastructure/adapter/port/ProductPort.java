package cn.fcr.infrastructure.adapter.port;

import cn.fcr.domain.order.adapter.port.IProductPort;
import cn.fcr.domain.order.model.entity.ProductEntity;
import cn.fcr.infrastructure.gateway.ProductRPC;
import cn.fcr.infrastructure.gateway.dto.ProductDTO;
import org.springframework.stereotype.Component;

/**
 * @author xiaolv
 * @date 2025/7/28 22:27
 * @description
 */
@Component
public class ProductPort implements IProductPort {

    private final ProductRPC productRPC;

    public ProductPort(ProductRPC productRPC) {
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
}
