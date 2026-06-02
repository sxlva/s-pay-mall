package cn.fcr.api;

import cn.fcr.api.response.Response;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 商城商品服务接口
 * 对外暴露商品查询、分类查询等接口
 */
public interface IMallProductService {

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
    Response<List<Map<String, Object>>> listProducts(
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
    Response<List<Map<String, Object>>> listCategories();
}