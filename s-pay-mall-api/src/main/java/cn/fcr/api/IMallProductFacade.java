package cn.fcr.api;

import cn.fcr.api.response.Response;
import cn.fcr.api.vo.CategoryVO;
import cn.fcr.api.vo.ProductVO;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商城商品外观接口
 * 对外暴露商品查询、分类查询等接口
 *
 * @author 傅崇睿
 */
public interface IMallProductFacade {

    /**
     * 查询商品列表
     * 支持多条件筛选：分类、关键词、价格区间、状态
     *
     * @param categoryId 分类ID（可选）
     * @param keyword 关键词，模糊匹配商品名称（可选）
     * @param minPrice 最低价格（可选）
     * @param maxPrice 最高价格（可选）
     * @param status 商品状态（可选）
     * @return 商品列表
     */
    Response<List<ProductVO>> listProducts(
            Long categoryId,
            String keyword,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Integer status);

    /**
     * 查询商品分类列表
     *
     * @return 分类列表，包含分类名称和商品数量
     */
    Response<List<CategoryVO>> listCategories();
}