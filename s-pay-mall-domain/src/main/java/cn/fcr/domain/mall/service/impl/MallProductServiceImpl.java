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
        return productRepository.deleteProduct(id);
    }

    public void validateProductForSale(Long productId, int quantity, BigDecimal orderPrice) {
        ProductEntity product = productRepository.findById(productId);
        if (product == null) {
            throw new IllegalArgumentException("商品不存在");
        }
        if (!product.isAvailable()) {
            throw new IllegalStateException("商品已下架");
        }
        if (!product.validateStock(quantity)) {
            throw new IllegalStateException("商品库存不足");
        }
        if (!product.validatePrice(orderPrice)) {
            throw new IllegalStateException("商品价格异常");
        }
    }

    public boolean reduceStockIfAvailable(Long productId, int quantity) {
        ProductEntity product = productRepository.findById(productId);
        if (product == null) return false;
        if (!product.canReduceStock(quantity)) {
            return false;
        }
        return productRepository.decreaseStock(productId, quantity) > 0;
    }

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