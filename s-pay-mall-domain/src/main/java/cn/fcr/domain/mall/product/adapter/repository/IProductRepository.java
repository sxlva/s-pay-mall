package cn.fcr.domain.mall.product.adapter.repository;

import cn.fcr.domain.mall.product.model.entity.ProductEntity;
import cn.fcr.domain.mall.product.model.valobj.CategoryVO;
import cn.fcr.domain.mall.product.model.valobj.ProductVO;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商品仓储接口，定义商品和分类持久化操作的抽象。
 *
 * @author 傅崇睿
 */
public interface IProductRepository {

    /**
     * 查询全部分类列表（按 ID 倒序）
     *
     * @return 分类 VO 列表
     */
    List<CategoryVO> findAllCategories();

    /**
     * 保存分类（新增或更新）
     * ID 为空时新增（状态默认启用），ID 非空时按非空字段局部更新
     *
     * @param name   分类名称（更新时可为 null，表示不修改）
     * @param status 分类状态（更新时可为 null，表示不修改）
     * @param id     分类ID，null 表示新增
     * @return 影响行数
     */
    int saveCategory(String name, Integer status, Long id);

    /**
     * 删除分类
     * 分类下存在关联商品时抛出 CategoryHasProductsException
     *
     * @param id 分类ID
     * @return 影响行数
     */
    int deleteCategory(Long id);

    /**
     * 统计分类下上架商品数量
     * 用于删除分类前检查是否存在关联商品
     *
     * @param categoryId 分类ID
     * @return 该分类下上架商品数量
     */
    long countProductsByCategoryId(Long categoryId);

    /**
     * 分页条件查询商品列表（联表查询分类信息）
     * 各条件均为可选，同时为 null 时查询全部
     *
     * @param categoryId 分类ID（可选）
     * @param keyword    商品名称关键字（可选）
     * @param minPrice   价格下限（可选）
     * @param maxPrice   价格上限（可选）
     * @param status     商品状态（可选）
     * @return 商品 VO 列表
     */
    List<ProductVO> findProducts(Long categoryId, String keyword, BigDecimal minPrice, BigDecimal maxPrice, Integer status);

    /**
     * 按 ID 查询单个商品
     *
     * @param id 商品ID
     * @return 商品实体，商品不存在时返回 null
     */
    ProductEntity findById(Long id);

    /**
     * 批量查询商品
     *
     * @param ids 商品ID列表
     * @return 商品实体列表
     */
    List<ProductEntity> findByIds(List<Long> ids);

    /**
     * 保存商品（新增或更新）
     * 新增时返回带自增ID的 Entity，更新时返回原 Entity
     *
     * @param product 商品实体
     * @return 保存后的商品实体（含ID）
     */
    ProductEntity saveProduct(ProductEntity product);

    /**
     * 删除商品
     * 商品下存在关联订单时抛出 ProductHasOrdersException
     *
     * @param id 商品ID
     * @return 影响行数
     */
    int deleteProduct(Long id);

    /**
     * 扣减商品库存（乐观锁：仅库存充足时扣减）
     * 用于支付成功后的库存扣减
     *
     * @param id       商品ID
     * @param quantity 扣减数量
     * @return 影响行数，0 表示库存不足或商品不存在
     */
    int decreaseStock(Long id, Integer quantity);

    /**
     * 增加商品库存
     * 用于订单取消或超时关单时恢复库存
     *
     * @param id       商品ID
     * @param quantity 增加数量
     * @return 影响行数，0 表示商品不存在
     */
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

    /**
     * 查询全部被商品引用的图片路径
     * 用于孤儿图片清理任务比对引用关系
     *
     * @return image_url 集合，无引用时返回空集合
     */
    java.util.Set<String> selectAllImageUrls();
}