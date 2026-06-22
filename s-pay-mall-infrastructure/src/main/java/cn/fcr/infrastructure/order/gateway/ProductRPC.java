package cn.fcr.infrastructure.order.gateway;

import cn.fcr.infrastructure.order.gateway.dto.ProductDTO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 商品RPC服务（已弃用，仅保留模拟实现）
 *
 * @author 傅崇睿
 * @deprecated 旧 Order Domain 占位实现，当前主流程已迁移至 Mall Domain，此类暂未启用
 */
@Service
public class ProductRPC {

    public ProductDTO queryProductByProductId(String productId){
        ProductDTO productVO = new ProductDTO();
        productVO.setProductId(productId);
        productVO.setProductName("测试商品");
        productVO.setProductDesc("这是一个测试商品");
        productVO.setPrice(new BigDecimal("1.68"));
        return productVO;
    }
}
