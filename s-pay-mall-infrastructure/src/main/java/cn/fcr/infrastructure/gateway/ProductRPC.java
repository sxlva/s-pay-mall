package cn.fcr.infrastructure.gateway;

import cn.fcr.infrastructure.gateway.dto.ProductDTO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * @description 商品RPC服务（模拟实现）
 * 
 * 【职责说明】
 * - 模拟商品服务的RPC调用
 * - 提供商品查询接口，返回模拟数据
 * - 实际生产环境应替换为真实的RPC调用
 * 
 * 【核心功能】
 * 1. queryProductByProductId(): 根据商品ID查询商品信息（模拟数据）
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
