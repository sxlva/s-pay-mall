package cn.fcr.domain.mall.product.service.impl;

import cn.fcr.domain.mall.product.adapter.repository.IProductRepository;
import cn.fcr.domain.mall.product.gateway.IStockGateway;
import cn.fcr.domain.mall.product.model.command.ProductSaveCommand;
import cn.fcr.domain.mall.product.model.entity.ProductEntity;
import cn.fcr.domain.mall.product.model.exception.CategoryHasProductsException;
import cn.fcr.domain.mall.product.model.exception.ProductHasOrdersException;
import cn.fcr.domain.mall.product.model.valobj.CategoryVO;
import cn.fcr.domain.mall.product.model.valobj.ProductVO;
import cn.fcr.domain.mall.product.service.IMallProductService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * 商品领域服务实现，负责分类管理、商品 CRUD 与删除前关联检查。
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
    public List<ProductVO> listProducts(Long categoryId, String keyword, BigDecimal minPrice, BigDecimal maxPrice, Integer status) {
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