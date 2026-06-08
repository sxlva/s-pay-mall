package cn.fcr.domain.mall.adapter.repository;

import cn.fcr.domain.mall.model.entity.ProductEntity;
import cn.fcr.domain.mall.model.valobj.CategoryVO;
import cn.fcr.domain.mall.model.valobj.ProductVO;

import java.math.BigDecimal;
import java.util.List;

public interface IProductRepository {

    List<CategoryVO> findAllCategories();

    int saveCategory(String name, Integer status, Long id);

    int deleteCategory(Long id);

    long countProductsByCategoryId(Long categoryId);

    List<ProductVO> findProducts(Long categoryId, String keyword, BigDecimal minPrice, BigDecimal maxPrice, Integer status);

    ProductEntity findById(Long id);

    int saveProduct(ProductEntity product);

    int deleteProduct(Long id);

    int decreaseStock(Long id, Integer quantity);

    int increaseStock(Long id, Integer quantity);
}