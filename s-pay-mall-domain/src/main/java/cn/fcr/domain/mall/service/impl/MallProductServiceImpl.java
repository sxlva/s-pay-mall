package cn.fcr.domain.mall.service.impl;

import cn.fcr.domain.mall.adapter.repository.IProductRepository;
import cn.fcr.domain.mall.gateway.IStockGateway;
import cn.fcr.domain.mall.model.command.ProductSaveCommand;
import cn.fcr.domain.mall.model.entity.ProductEntity;
import cn.fcr.domain.mall.model.exception.CategoryHasProductsException;
import cn.fcr.domain.mall.model.exception.ProductHasOrdersException;
import cn.fcr.domain.mall.model.valobj.CategoryVO;
import cn.fcr.domain.mall.model.valobj.ProductVO;
import cn.fcr.domain.mall.service.IMallProductService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * 商品领域服务实现，负责分类管理、商品 CRUD、库存校验与库存增减。
 *
 * @author 傅崇睿
 */
@Slf4j
public class MallProductServiceImpl implements IMallProductService {

    private final IProductRepository productRepository;
    private final IStockGateway stockGateway;

    public MallProductServiceImpl(IProductRepository productRepository, IStockGateway stockGateway) {
        this.productRepository = productRepository;
        this.stockGateway = stockGateway;
    }

    @Override
    public List<CategoryVO> listCategory() {
        return productRepository.findAllCategories();
    }

    @Override
    public int saveCategory(Map<String, Object> category) {
        Long id = parseLong(category.get("id"));
        String name = (String) category.get("name");
        Integer status = parseInteger(category.get("status"));
        return productRepository.saveCategory(name, status, id);
    }

    @Override
    public int deleteCategory(Long id) {
        long count = productRepository.countProductsByCategoryId(id);
        if (count > 0) {
            throw new CategoryHasProductsException("该分类下仍有关联商品，无法删除！");
        }
        return productRepository.deleteCategory(id);
    }

    @Override
    public List<ProductVO> listProducts(Long categoryId, String category, String keyword, BigDecimal minPrice, BigDecimal maxPrice, Integer status) {
        log.info("【商品查询】分类过滤: categoryId=" + categoryId + ", keyword=" + keyword);

        List<ProductVO> result = productRepository.findProducts(categoryId, keyword, minPrice, maxPrice, status);

        log.info("【商品查询】完成，数量: " + result.size());
        return result;
    }

    @Override
    public int saveProduct(ProductSaveCommand command) {
        ProductEntity product = ProductEntity.builder()
                .id(command.getId())
                .categoryId(command.getCategoryId())
                .name(command.getName())
                .description(command.getDescription())
                .price(command.getPrice())
                .stock(command.getStock())
                .status(command.getStatus())
                .build();
        ProductEntity saved = productRepository.saveProduct(product);

        // DB 保存成功后同步库存到 Redis
        stockGateway.syncStockFromDB(saved.getId());

        return 1;
    }

    @Override
    public int deleteProduct(Long id) {
        long count = productRepository.countOrderItemsByProductId(id);
        if (count > 0) {
            throw new ProductHasOrdersException("该商品下仍有关联订单，无法删除！");
        }
        return productRepository.deleteProduct(id);
    }

    /**
     * 验证商品是否可销售
     *
     * @param productId 商品ID
     * @param quantity 购买数量
     * @param orderPrice 订单价格
     * @throws IllegalArgumentException 商品不存在
     * @throws IllegalStateException 商品已下架、库存不足或价格异常
     */
    public void validateProductForSale(Long productId, int quantity, BigDecimal orderPrice) {
        ProductEntity product = productRepository.findById(productId);
        if (product == null) {
            throw new IllegalArgumentException("商品不存在");
        }
        product.validateForSale(quantity, orderPrice);
    }

    /**
     * 扣减库存（如果库存充足）
     *
     * @param productId 商品ID
     * @param quantity 扣减数量
     * @return true=扣减成功，false=库存不足或商品不存在
     */
    public boolean reduceStockIfAvailable(Long productId, int quantity) {
        ProductEntity product = productRepository.findById(productId);
        if (product == null) {
            return false;
        }
        if (!product.canReduceStock(quantity)) {
            return false;
        }
        return productRepository.decreaseStock(productId, quantity) > 0;
    }

    /**
     * 恢复库存
     *
     * @param productId 商品ID
     * @param quantity 恢复数量
     */
    public void restoreStock(Long productId, int quantity) {
        productRepository.increaseStock(productId, quantity);
    }

    private Long parseLong(Object value) {
        if (value == null) return null;
        if (value instanceof Long) return (Long) value;
        if (value instanceof Integer) return ((Integer) value).longValue();
        return Long.valueOf(value.toString());
    }

    private Integer parseInteger(Object value) {
        if (value == null) return null;
        if (value instanceof Integer) return (Integer) value;
        return Integer.valueOf(value.toString());
    }
}