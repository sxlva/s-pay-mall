package cn.fcr.domain.mall.product.service;

import cn.fcr.domain.mall.product.model.command.ProductSaveCommand;
import cn.fcr.domain.mall.product.model.valobj.CategoryVO;
import cn.fcr.domain.mall.product.model.valobj.ProductVO;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 商品领域服务接口，定义商品分类管理和商品 CRUD 的抽象。
 *
 * @author 傅崇睿
 */
public interface IMallProductService {

    /**
     * 查询全部分类列表
     *
     * @return 分类 VO 列表
     */
    List<CategoryVO> listCategory();

    /**
     * 新增或编辑分类
     *
     * @param category 分类信息，包含 id（为空新增，非空更新）、name、status
     * @return 影响行数
     */
    int saveCategory(Map<String, Object> category);

    /**
     * 删除分类
     * 分类下仍存在商品时抛出 {@code CategoryHasProductsException} 拒绝删除
     *
     * @param id 分类ID
     * @return 影响行数
     */
    int deleteCategory(Long id);

    /**
     * 条件查询商品列表
     * 支持按分类、关键词、价格区间、状态筛选
     *
     * @param categoryId 分类ID（可选）
     * @param keyword    关键词，匹配商品名称（可选）
     * @param minPrice   最低价格（可选）
     * @param maxPrice   最高价格（可选）
     * @param status     商品状态（可选）
     * @return 商品 VO 列表
     */
    List<ProductVO> listProducts(Long categoryId, String keyword, BigDecimal minPrice, BigDecimal maxPrice, Integer status);

    /**
     * 新增或编辑商品
     * 保存成功后同步商品库存到 Redis
     *
     * @param command 商品保存命令对象
     * @return 影响行数
     */
    int saveProduct(ProductSaveCommand command);

    /**
     * 删除商品
     * 商品下仍存在关联订单时抛出 {@code ProductHasOrdersException} 拒绝删除
     *
     * @param id 商品ID
     * @return 影响行数
     */
    int deleteProduct(Long id);
}