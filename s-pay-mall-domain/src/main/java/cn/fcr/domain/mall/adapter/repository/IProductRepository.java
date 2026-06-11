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

    /**
     * 查询所有上架商品的ID列表
     * 用于库存预热时批量同步
     *
     * @return 上架商品ID列表（status=1）
     */
    List<Long> queryAllActiveProductIds();

    /**
     * 查询单个商品的库存数量
     * 用于库存同步时从数据库获取最新库存
     *
     * @param productId 商品ID
     * @return 库存数量，若商品不存在返回 null
     */
    Integer queryStockByProductId(Long productId);

    /**
     * 统计商品关联的订单数量
     * 用于删除商品前检查是否存在关联订单
     *
     * @param productId 商品ID
     * @return 关联订单数量
     */
    long countOrderItemsByProductId(Long productId);
}