package cn.fcr.api;

import cn.fcr.api.dto.CartAddRequestDTO;
import cn.fcr.api.dto.OrderCreateRequestDTO;
import cn.fcr.api.response.Response;
import cn.fcr.api.vo.CartItemVO;
import cn.fcr.api.vo.OrderCreateVO;
import cn.fcr.api.vo.OrderVO;

import java.util.List;

/**
 * 商城订单外观接口
 * 对外暴露购物车和订单相关的操作接口
 */
public interface IMallOrderFacade {

    /**
     * 添加商品到购物车
     *
     * @param userId 用户ID
     * @param request 购物车请求（商品ID、数量）
     * @return 成功返回购物车项数量
     */
    Response<Integer> addCart(Long userId, CartAddRequestDTO request);

    /**
     * 获取用户购物车列表
     *
     * @param userId 用户ID
     * @return 购物车商品列表
     */
    Response<List<CartItemVO>> listCart(Long userId);

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
    Response<OrderCreateVO> createOrder(Long userId, OrderCreateRequestDTO request);

    /**
     * 获取用户订单列表
     *
     * @param userId 用户ID
     * @param status 订单状态筛选（可选）
     * @param startTime 开始时间（可选）
     * @param endTime 结束时间（可选）
     * @return 订单列表
     */
    Response<List<OrderVO>> listOrders(Long userId, String status, String startTime, String endTime);
}