package cn.fcr.infrastructure.dao;

import cn.fcr.domain.mall.model.valobj.ProductVO;
import cn.fcr.infrastructure.dao.po.Product;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商品数据访问接口
 * 继承 MyBatis-Plus BaseMapper，提供商品的 CRUD 操作
 * 自定义方法支持库存扣减、关联分类查询和条件搜索
 */
@Mapper
public interface IProductDao extends BaseMapper<Product> {

    /**
     * 扣减商品库存
     * 使用乐观锁思想，通过 WHERE 条件确保库存充足时才扣减
     * 防止超卖问题
     *
     * @param id 商品ID
     * @param quantity 扣减数量
     * @return 影响行数，0 表示库存不足或商品不存在
     */
    @Update("UPDATE product SET stock = stock - #{quantity}, update_time = NOW() WHERE id = #{id} AND stock >= #{quantity}")
    int decreaseStock(@Param("id") Long id, @Param("quantity") Integer quantity);

    /**
     * 增加商品库存
     * 用于订单取消或退货时恢复库存
     *
     * @param id 商品ID
     * @param quantity 增加数量
     * @return 影响行数
     */
    @Update("UPDATE product SET stock = stock + #{quantity}, update_time = NOW() WHERE id = #{id}")
    int increaseStock(@Param("id") Long id, @Param("quantity") Integer quantity);

    /**
     * 根据ID查询商品详情，附带分类名称
     * 使用 LEFT JOIN 关联 category 表获取分类信息
     *
     * @param id 商品ID
     * @return 包含分类名称的商品视图对象
     */
    @Select("SELECT p.*, c.name as category_name FROM product p LEFT JOIN category c ON p.category_id = c.id WHERE p.id = #{id}")
    ProductVO selectProductWithCategory(@Param("id") Long id);

    /**
     * 条件搜索商品列表，附带分类名称
     * 支持按分类、关键词、价格区间、状态筛选
     * 使用动态 SQL 构建查询条件
     *
     * @param categoryId 分类ID（可选）
     * @param keyword 关键词，匹配商品名称（可选）
     * @param minPrice 最低价格（可选）
     * @param maxPrice 最高价格（可选）
     * @param status 商品状态（可选）
     * @return 符合条件的商品列表
     */
    @Select("<script>SELECT p.*, c.name as category_name FROM product p LEFT JOIN category c ON p.category_id = c.id WHERE 1=1" +
            "<if test='categoryId != null'> AND p.category_id = #{categoryId}</if>" +
            "<if test='keyword != null and keyword != \"\"'> AND p.name LIKE CONCAT('%', #{keyword}, '%')</if>" +
            "<if test='minPrice != null'> AND p.price &gt;= #{minPrice}</if>" +
            "<if test='maxPrice != null'> AND p.price &lt;= #{maxPrice}</if>" +
            "<if test='status != null'> AND p.status = #{status}</if>" +
            " ORDER BY p.id DESC</script>")
    List<ProductVO> selectProductsWithCategory(
            @Param("categoryId") Long categoryId,
            @Param("keyword") String keyword,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("status") Integer status);

    /**
     * 查询所有上架商品的ID列表
     * 用于库存预热时批量同步
     *
     * @return 上架商品ID列表（status=1）
     */
    @Select("SELECT id FROM product WHERE status = 1")
    List<Long> selectAllActiveProductIds();

    /**
     * 查询单个商品的库存数量
     * 用于库存同步时从数据库获取最新库存
     *
     * @param productId 商品ID
     * @return 库存数量，若商品不存在或已下架返回 null
     */
    @Select("SELECT stock FROM product WHERE id = #{productId} AND status = 1")
    Integer selectStockByProductId(@Param("productId") Long productId);
}