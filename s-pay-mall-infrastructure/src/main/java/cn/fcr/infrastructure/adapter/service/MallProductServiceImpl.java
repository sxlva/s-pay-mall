package cn.fcr.infrastructure.adapter.service;

import cn.fcr.domain.mall.model.command.ProductSaveCommand;
import cn.fcr.domain.mall.model.entity.ProductEntity;
import cn.fcr.domain.mall.model.valobj.CategoryVO;
import cn.fcr.domain.mall.model.valobj.ProductVO;
import cn.fcr.domain.mall.service.IMallProductService;
import cn.fcr.infrastructure.dao.ICategoryDao;
import cn.fcr.infrastructure.dao.IProductDao;
import cn.fcr.infrastructure.dao.po.Category;
import cn.fcr.infrastructure.dao.po.Product;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class MallProductServiceImpl implements IMallProductService {

    @Resource
    private ICategoryDao categoryDao;

    @Resource
    private IProductDao productDao;

    @Override
    public List<CategoryVO> listCategory() {
        LambdaQueryWrapper<Category> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(Category::getId);
        return categoryDao.selectList(queryWrapper).stream()
                .map(this::toCategoryVO)
                .toList();
    }

    @Override
    public int saveCategory(Map<String, Object> category) {
        Long id = parseLong(category.get("id"));
        String name = (String) category.get("name");
        Integer status = parseInteger(category.get("status"));
        if (id == null) {
            Category cat = new Category();
            cat.setName(name);
            cat.setStatus(status != null ? status : 1);
            cat.setCreateTime(LocalDateTime.now());
            cat.setUpdateTime(LocalDateTime.now());
            return categoryDao.insert(cat);
        }
        LambdaUpdateWrapper<Category> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Category::getId, id);
        updateWrapper.set(Category::getName, name);
        updateWrapper.set(Category::getStatus, status);
        updateWrapper.set(Category::getUpdateTime, LocalDateTime.now());
        return categoryDao.update(null, updateWrapper);
    }

    @Override
    public int deleteCategory(Long id) {
        // 检查该分类下是否有关联商品
        LambdaQueryWrapper<Product> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Product::getCategoryId, id);
        queryWrapper.eq(Product::getStatus, 1); // 只检查上架状态的商品
        long count = productDao.selectCount(queryWrapper);
        
        if (count > 0) {
            throw new IllegalStateException("该分类下仍有关联商品，无法删除！");
        }
        
        return categoryDao.deleteById(id);
    }

    /**
     * 分类规约守卫 - 确保商品查询时的分类过滤是严格的
     */
    @lombok.AllArgsConstructor
    @lombok.Getter
    private static class CategorySpecification {
        private final Long categoryId;
        private final String categoryName;
        
        /**
         * 判断是否有分类限制
         */
        public boolean hasRestriction() {
            return categoryId != null || categoryName != null;
        }
        
        /**
         * 获取分类限制描述
         */
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
        // 构建分类规约
        CategorySpecification spec = new CategorySpecification(categoryId, category);
        
        log.info("【商品查询】分类规约: {}", spec.getDescription());
        
        // 确保 categoryId 为空时也能正确查询（返回全部）
        // 由 Repository 层直接进行强过滤，严禁在应用层做胶水组装
        List<ProductVO> result = productDao.selectProductsWithCategory(categoryId, keyword, minPrice, maxPrice, status);
        
        log.info("【商品查询】完成，数量: {}, 分类过滤: {}", result.size(), spec.hasRestriction() ? "已应用" : "未应用");
        return result;
    }

    @Override
    public int saveProduct(ProductSaveCommand command) {
        Long id = command.getId();
        Long categoryId = command.getCategoryId();
        String name = command.getName();
        String description = command.getDescription();
        BigDecimal price = command.getPrice();
        Integer stock = command.getStock();
        Integer statusVal = command.getStatus();

        if (id == null) {
            Product prod = new Product();
            prod.setCategoryId(categoryId);
            prod.setName(name);
            prod.setDescription(description);
            prod.setPrice(price);
            prod.setStock(stock);
            prod.setStatus(statusVal != null ? statusVal : 1);
            prod.setCreateTime(LocalDateTime.now());
            prod.setUpdateTime(LocalDateTime.now());
            return productDao.insert(prod);
        }
        LambdaUpdateWrapper<Product> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Product::getId, id);
        updateWrapper.set(Product::getCategoryId, categoryId);
        updateWrapper.set(Product::getName, name);
        updateWrapper.set(Product::getDescription, description);
        updateWrapper.set(Product::getPrice, price);
        updateWrapper.set(Product::getStock, stock);
        updateWrapper.set(Product::getStatus, statusVal);
        updateWrapper.set(Product::getUpdateTime, LocalDateTime.now());
        return productDao.update(null, updateWrapper);
    }

    @Override
    public int deleteProduct(Long id) {
        return productDao.deleteById(id);
    }

    public void validateProductForSale(Long productId, int quantity, BigDecimal orderPrice) {
        Product product = productDao.selectById(productId);
        if (product == null) {
            throw new IllegalArgumentException("商品不存在");
        }
        ProductEntity entity = buildProductEntity(product);
        if (!entity.isAvailable()) {
            throw new IllegalStateException("商品已下架");
        }
        if (!entity.validateStock(quantity)) {
            throw new IllegalStateException("商品库存不足");
        }
        if (!entity.validatePrice(orderPrice)) {
            throw new IllegalStateException("商品价格异常");
        }
    }

    public boolean reduceStockIfAvailable(Long productId, int quantity) {
        Product product = productDao.selectById(productId);
        if (product == null) return false;
        ProductEntity entity = buildProductEntity(product);
        if (!entity.canReduceStock(quantity)) {
            return false;
        }
        LambdaUpdateWrapper<Product> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Product::getId, productId);
        updateWrapper.set(Product::getStock, entity.getStock() - quantity);
        updateWrapper.set(Product::getUpdateTime, LocalDateTime.now());
        return productDao.update(null, updateWrapper) > 0;
    }

    public void restoreStock(Long productId, int quantity) {
        Product product = productDao.selectById(productId);
        if (product == null) return;
        ProductEntity entity = buildProductEntity(product);
        entity.restoreStock(quantity);
        LambdaUpdateWrapper<Product> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Product::getId, productId);
        updateWrapper.set(Product::getStock, entity.getStock());
        updateWrapper.set(Product::getUpdateTime, LocalDateTime.now());
        productDao.update(null, updateWrapper);
    }

    private ProductEntity buildProductEntity(Product product) {
        return ProductEntity.builder()
                .id(product.getId())
                .categoryId(product.getCategoryId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stock(product.getStock())
                .status(product.getStatus())
                .category(product.getCategory())
                .createTime(product.getCreateTime())
                .updateTime(product.getUpdateTime())
                .build();
    }

    private CategoryVO toCategoryVO(Category cat) {
        return CategoryVO.builder()
                .id(cat.getId())
                .name(cat.getName())
                .status(cat.getStatus())
                .createTime(cat.getCreateTime())
                .updateTime(cat.getUpdateTime())
                .build();
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

    private BigDecimal parseBigDecimal(Object value) {
        if (value == null) return null;
        if (value instanceof BigDecimal) return (BigDecimal) value;
        return new BigDecimal(value.toString());
    }
}