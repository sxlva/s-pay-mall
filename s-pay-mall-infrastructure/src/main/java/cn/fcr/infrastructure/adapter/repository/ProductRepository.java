package cn.fcr.infrastructure.adapter.repository;

import cn.fcr.domain.mall.adapter.repository.IProductRepository;
import cn.fcr.domain.mall.model.entity.ProductEntity;
import cn.fcr.domain.mall.model.valobj.CategoryVO;
import cn.fcr.domain.mall.model.valobj.ProductVO;
import cn.fcr.infrastructure.dao.ICategoryDao;
import cn.fcr.infrastructure.dao.IOrderItemDao;
import cn.fcr.infrastructure.dao.IProductDao;
import cn.fcr.infrastructure.dao.po.Category;
import cn.fcr.infrastructure.dao.po.OrderItem;
import cn.fcr.infrastructure.dao.po.Product;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Repository
public class ProductRepository implements IProductRepository {

    @Resource
    private ICategoryDao categoryDao;

    @Resource
    private IProductDao productDao;

    @Resource
    private IOrderItemDao orderItemDao;

    @Override
    public List<CategoryVO> findAllCategories() {
        LambdaQueryWrapper<Category> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(Category::getId);
        return categoryDao.selectList(queryWrapper).stream()
                .map(this::toCategoryVO)
                .collect(Collectors.toList());
    }

    @Override
    public int saveCategory(String name, Integer status, Long id) {
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
        if (name != null) {
            updateWrapper.set(Category::getName, name);
        }
        if (status != null) {
            updateWrapper.set(Category::getStatus, status);
        }
        updateWrapper.set(Category::getUpdateTime, LocalDateTime.now());
        return categoryDao.update(null, updateWrapper);
    }

    @Override
    public int deleteCategory(Long id) {
        try {
            return categoryDao.deleteById(id);
        } catch (Exception e) {
            throw new cn.fcr.domain.mall.model.exception.CategoryHasProductsException("该分类下仍有关联商品，无法删除");
        }
    }

    @Override
    public long countProductsByCategoryId(Long categoryId) {
        LambdaQueryWrapper<Product> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Product::getCategoryId, categoryId);
        queryWrapper.eq(Product::getStatus, 1);
        return productDao.selectCount(queryWrapper);
    }

    @Override
    public List<ProductVO> findProducts(Long categoryId, String keyword, BigDecimal minPrice, BigDecimal maxPrice, Integer status) {
        return productDao.selectProductsWithCategory(categoryId, keyword, minPrice, maxPrice, status);
    }

    @Override
    public ProductEntity findById(Long id) {
        Product product = productDao.selectById(id);
        if (product == null) return null;
        return buildProductEntity(product);
    }

    @Override
    public int saveProduct(ProductEntity product) {
        if (product.getId() == null) {
            Product prod = toProductPO(product);
            prod.setCreateTime(LocalDateTime.now());
            prod.setUpdateTime(LocalDateTime.now());
            return productDao.insert(prod);
        }
        LambdaUpdateWrapper<Product> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Product::getId, product.getId());
        updateWrapper.set(Product::getCategoryId, product.getCategoryId());
        updateWrapper.set(Product::getName, product.getName());
        updateWrapper.set(Product::getDescription, product.getDescription());
        updateWrapper.set(Product::getPrice, product.getPrice());
        updateWrapper.set(Product::getStock, product.getStock());
        updateWrapper.set(Product::getStatus, product.getStatus());
        updateWrapper.set(Product::getUpdateTime, LocalDateTime.now());
        return productDao.update(null, updateWrapper);
    }

    @Override
    public int deleteProduct(Long id) {
        try {
            return productDao.deleteById(id);
        } catch (Exception e) {
            throw new cn.fcr.domain.mall.model.exception.ProductHasOrdersException("该商品下仍有关联订单，无法删除");
        }
    }

    @Override
    public int decreaseStock(Long id, Integer quantity) {
        return productDao.decreaseStock(id, quantity);
    }

    @Override
    public int increaseStock(Long id, Integer quantity) {
        return productDao.increaseStock(id, quantity);
    }

    @Override
    public List<Long> queryAllActiveProductIds() {
        return productDao.selectAllActiveProductIds();
    }

    @Override
    public Integer queryStockByProductId(Long productId) {
        return productDao.selectStockByProductId(productId);
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

    private Product toProductPO(ProductEntity entity) {
        Product product = new Product();
        product.setId(entity.getId());
        product.setCategoryId(entity.getCategoryId());
        product.setName(entity.getName());
        product.setDescription(entity.getDescription());
        product.setPrice(entity.getPrice());
        product.setStock(entity.getStock());
        product.setStatus(entity.getStatus());
        product.setCategory(entity.getCategory());
        return product;
    }

    @Override
    public long countOrderItemsByProductId(Long productId) {
        LambdaQueryWrapper<OrderItem> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OrderItem::getProductId, productId);
        return orderItemDao.selectCount(queryWrapper);
    }
}