package cn.fcr.api;

import cn.fcr.api.dto.CategorySaveRequestDTO;
import cn.fcr.api.dto.ProductSaveRequest;
import cn.fcr.api.dto.UserSaveRequestDTO;
import cn.fcr.api.response.Response;
import cn.fcr.api.vo.CategoryRatioVO;
import cn.fcr.api.vo.CategoryVO;
import cn.fcr.api.vo.OrderVO;
import cn.fcr.api.vo.ProductVO;
import cn.fcr.api.vo.SalesTrendVO;
import cn.fcr.api.vo.UserVO;

import java.math.BigDecimal;
import java.util.List;

/**
 * 管理员外观接口
 * 对外暴露后台管理相关的接口，包括用户管理、商品管理、订单管理、统计分析等
 * 所有接口需要 ADMIN 角色权限
 */
public interface IAdminFacade {

    // ==================== 用户管理 ====================

    /**
     * 查询用户列表
     *
     * @param username 用户名筛选（可选）
     * @param status 用户状态筛选（可选）
     * @param roleCode 角色编码筛选（可选）
     * @return 用户列表
     */
    Response<List<UserVO>> listUsers(String username, Integer status, String roleCode);

    /**
     * 保存用户（新增或更新）
     *
     * @param request 用户信息
     * @return 成功返回影响行数
     */
    Response<Integer> saveUser(UserSaveRequestDTO request);

    /**
     * 更新用户状态
     *
     * @param userId 用户ID
     * @param status 新状态
     * @return 成功返回影响行数
     */
    Response<Integer> updateUserStatus(Long userId, Integer status);

    /**
     * 删除用户
     *
     * @param userId 用户ID
     * @return 成功返回影响行数
     */
    Response<Integer> deleteUser(Long userId);

    // ==================== 分类管理 ====================

    /**
     * 查询分类列表
     *
     * @return 分类列表
     */
    Response<List<CategoryVO>> listCategories();

    /**
     * 保存分类（新增或更新）
     *
     * @param request 分类信息
     * @return 成功返回影响行数
     */
    Response<Integer> saveCategory(CategorySaveRequestDTO request);

    /**
     * 删除分类
     *
     * @param categoryId 分类ID
     * @return 成功返回影响行数
     */
    Response<Integer> deleteCategory(Long categoryId);

    // ==================== 商品管理 ====================

    /**
     * 查询商品列表
     *
     * @param categoryId 分类ID（可选）
     * @param keyword 关键词（可选）
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
     * 保存商品（新增或更新）
     *
     * @param request 商品信息
     * @return 成功返回影响行数
     */
    Response<Integer> saveProduct(ProductSaveRequest request);

    /**
     * 删除商品
     *
     * @param productId 商品ID
     * @return 成功返回影响行数
     */
    Response<Integer> deleteProduct(Long productId);

    // ==================== 订单管理 ====================

    /**
     * 查询订单列表
     *
     * @param userId 用户ID筛选（可选）
     * @param status 订单状态筛选（可选）
     * @param startTime 开始时间（可选）
     * @param endTime 结束时间（可选）
     * @return 订单列表
     */
    Response<List<OrderVO>> listOrders(
            Long userId,
            String status,
            String startTime,
            String endTime);

    /**
     * 订单发货
     *
     * @param orderId 订单ID
     * @return 成功返回影响行数
     */
    Response<Integer> deliverOrder(Long orderId);

    /**
     * 取消订单
     *
     * @param orderId 订单ID
     * @return 成功返回影响行数
     */
    Response<Integer> cancelOrder(Long orderId);

    /**
     * 删除订单
     *
     * @param orderId 订单ID
     * @return 成功返回影响行数
     */
    Response<Integer> deleteOrder(Long orderId);

    // ==================== 统计分析 ====================

    /**
     * 获取销售趋势数据
     * 返回最近7天的销售数据
     *
     * @return 包含日期、销售额、订单数的列表
     */
    Response<List<SalesTrendVO>> getSalesTrend();

    /**
     * 获取分类销售占比
     *
     * @return 包含分类名称和商品数量的列表
     */
    Response<List<CategoryRatioVO>> getCategoryRatio();
}