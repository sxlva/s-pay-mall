package cn.fcr.api;

import cn.fcr.api.dto.CartAddRequest;
import cn.fcr.api.dto.OrderCreateRequest;
import cn.fcr.api.response.Response;

import java.util.List;
import java.util.Map;

/**
 * 商城订单服务接口
 * 对外暴露购物车和订单相关的操作接口
 */
public interface IMallOrderService {

    /**
     * 添加商品到购物车
     *
     * @param userId 用户ID
     * @param request 购物车请求（商品ID、数量）
     * @return 成功返回购物车项数量
     */
    Response<Integer> addCart(Long userId, CartAddRequest request);

    /**
     * 获取用户购物车列表
     *
     * @param userId 用户ID
     * @return 购物车商品列表
     */
    Response<List<Map<String, Object>>> listCart(Long userId);

    /**
     * 删除购物车商品
     *
     * @param userId 用户ID
     * @param itemId 购物车项ID
     * @return 成功返回影响行数
     */
    Response<Integer> deleteCartItem(Long userId, Long itemId);

    /**
     * 创建订单
     * 根据购物车商品创建订单，返回支付链接
     *
     * @param userId 用户ID
     * @param request 订单请求（收货地址等）
     * @return 订单信息和支付链接
     */
    Response<Map<String, Object>> createOrder(Long userId, OrderCreateRequest request);

    /**
     * 获取用户订单列表
     *
     * @param userId 用户ID
     * @param status 订单状态筛选（可选）
     * @param startTime 开始时间（可选）
     * @param endTime 结束时间（可选）
     * @return 订单列表
     */
    Response<List<Map<String, Object>>> listOrders(Long userId, String status, String startTime, String endTime);
}