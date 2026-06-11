package cn.fcr.domain.mall.service.impl;

import cn.fcr.domain.mall.adapter.repository.IProductRepository;
import cn.fcr.domain.mall.model.command.ProductSaveCommand;
import cn.fcr.domain.mall.model.entity.ProductEntity;
import cn.fcr.domain.mall.model.valobj.CategoryVO;
import cn.fcr.domain.mall.model.valobj.ProductVO;
import cn.fcr.domain.mall.service.IMallProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class MallProductServiceImpl implements IMallProductService {

    @Resource
    private IProductRepository productRepository;

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
            throw new IllegalStateException("该分类下仍有关联商品，无法删除！");
        }
        return productRepository.deleteCategory(id);
    }

    private static class CategorySpecification {
        private final Long categoryId;
        private final String categoryName;

        public CategorySpecification(Long categoryId, String categoryName) {
            this.categoryId = categoryId;
            this.categoryName = categoryName;
        }

        public boolean hasRestriction() {
            return categoryId != null || categoryName != null;
        }

        public String getDescription() {
            if (categoryId != null) {
                return "categoryId=" + categoryId;
            }
            if (categoryName != null) {
                return "categoryName=" + categoryName;
            }
            return "无限制";
        }
    }

    @Override
    public List<ProductVO> listProducts(Long categoryId, String category, String keyword, BigDecimal minPrice, BigDecimal maxPrice, Integer status) {
        CategorySpecification spec = new CategorySpecification(categoryId, category);
        log.info("【商品查询】分类规约: {}", spec.getDescription());

        List<ProductVO> result = productRepository.findProducts(categoryId, keyword, minPrice, maxPrice, status);

        log.info("【商品查询】完成，数量: {}, 分类过滤: {}", result.size(), spec.hasRestriction() ? "已应用" : "未应用");
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
        return productRepository.saveProduct(product);
    }

    @Override
    public int deleteProduct(Long id) {
        long count = productRepository.countOrderItemsByProductId(id);
        if (count > 0) {
            throw new IllegalStateException("该商品下仍有关联订单，无法删除！");
        }
        return productRepository.deleteProduct(id);
    }

    /**
     * 验证商品是否可销售
     * 【DDD 重构】职责单一：Service 仅负责流程编排，业务逻辑由 Entity 执行
     *
     * @param productId 商品ID
     * @param quantity 购买数量
     * @param orderPrice 订单价格
     * @throws IllegalArgumentException 商品不存在
     * @throws IllegalStateException 商品已下架、库存不足或价格异常
     */
    public void validateProductForSale(Long productId, int quantity, BigDecimal orderPrice) {
        // 1. 查询实体（流程编排）
        ProductEntity product = productRepository.findById(productId);
        if (product == null) {
            throw new IllegalArgumentException("商品不存在");
        }

        // 2. 调用实体方法执行业务验证（充血模型）
        product.validateForSale(quantity, orderPrice);
    }

    /**
     * 扣减库存（如果库存充足）
     * 【DDD 重构】职责单一：Service 仅负责流程编排
     *
     * @param productId 商品ID
     * @param quantity 扣减数量
     * @return true=扣减成功，false=库存不足或商品不存在
     */
    public boolean reduceStockIfAvailable(Long productId, int quantity) {
        // 1. 查询实体（流程编排）
        ProductEntity product = productRepository.findById(productId);
        if (product == null) {
            return false;
        }

        // 2. 调用实体方法判断是否可扣减（充血模型）
        if (!product.canReduceStock(quantity)) {
            return false;
        }

        // 3. 执行持久化（流程编排）
        return productRepository.decreaseStock(productId, quantity) > 0;
    }

    /**
     * 恢复库存
     * 【DDD 重构】职责单一：Service 仅负责流程编排
     *
     * @param productId 商品ID
     * @param quantity 恢复数量
     */
    public void restoreStock(Long productId, int quantity) {
        // 直接调用 Repository 执行持久化（无业务逻辑）
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