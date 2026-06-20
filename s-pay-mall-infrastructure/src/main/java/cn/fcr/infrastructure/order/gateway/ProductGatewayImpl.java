package cn.fcr.infrastructure.order.gateway;

import cn.fcr.domain.mall.gateway.IStockGateway;
import cn.fcr.domain.order.gateway.IProductGateway;
import cn.fcr.domain.order.model.entity.ProductEntity;
import cn.fcr.infrastructure.dao.IProductDao;
import cn.fcr.infrastructure.order.gateway.dto.ProductDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import org.springframework.beans.factory.annotation.Autowired;
import javax.annotation.Resource;

/**
 * @description 产品网关实现
 *
 * 【职责说明】
 * - 处理商品相关的网关操作
 * - 实现领域层定义的IProductGateway接口
 * - 负责查询商品信息、恢复商品库存等
 *
 * 【核心功能】
 * 1. queryProductByProductId(): 根据商品ID查询商品信息
 * 2. restoreStock(): 恢复商品库存（MySQL + Redis）
 *
 * 【依赖说明】
 * - ProductRPC: 商品RPC服务
 * - IProductDao: 商品数据访问接口
 * - IStockGateway: 库存网关接口
 */
@Slf4j
@Component
public class ProductGatewayImpl implements IProductGateway {

    private final ProductRPC productRPC;

    @Resource
    private IProductDao productDao;

    @Resource
    private IStockGateway stockGateway;

    @Autowired
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

            // 1. 恢复 MySQL 库存
            productDao.increaseStock(id, quantity);
            log.info("MySQL 库存恢复成功: productId={}, quantity={}", productId, quantity);

            // 2. 恢复 Redis 库存（兜底方案）
            stockGateway.restoreStock(id, quantity);

        } catch (NumberFormatException e) {
            log.error("商品ID格式错误: productId={}", productId, e);
            throw new RuntimeException("商品ID格式错误: " + productId, e);
        }
    }
}
