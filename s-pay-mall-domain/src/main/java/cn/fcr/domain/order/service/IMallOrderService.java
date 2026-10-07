package cn.fcr.domain.order.service;

import cn.fcr.domain.order.model.entity.OrderEntity;
import cn.fcr.domain.mall.cart.model.valobj.CartItemVO;
import cn.fcr.domain.order.model.valobj.OrderCreateVO;
import cn.fcr.domain.order.model.valobj.OrderVO;

import java.util.List;

/**
 * 订单领域服务接口，定义订单创建、支付、发货、取消等核心业务流程抽象。
 *
 * @author 傅崇睿
 */
public interface IMallOrderService {

    /**
     * 预检查库存并扣减
     * 遍历购物车商品，检查库存是否充足，然后执行 Redis 原子扣减。
     * 不做异常捕获补偿，补偿逻辑由 Application 层统一编排。
     *
     * @param cart 购物车商品列表
     * @return 已成功扣减库存的商品列表（用于失败时恢复）
     */
    List<CartItemVO> checkAndDeductStock(List<CartItemVO> cart);

    /**
     * 创建订单实体并落库
     * 仅负责 OrderEntity 的创建与 saveOrder()，不涉及支付链接生成。
     *
     * @param userId  用户ID
     * @param address 收货地址
     * @param cart    购物车商品列表
     * @return 保存后的订单实体
     */
    OrderEntity buildAndSaveOrder(Long userId, String address, List<CartItemVO> cart);

    /**
     * 恢复已扣减的库存
     * 当订单创建流程失败时，由 Application 层调用此方法进行补偿。
     *
     * @param deductedItems 已扣减库存的商品列表
     */
    void restoreDeductedStock(List<CartItemVO> deductedItems);

    /**
     * 查询订单列表
     * 用户端按用户过滤，管理端可组合状态与时间范围筛选
     *
     * @param userId 用户ID
     * @param status 订单状态（可选）
     * @param start  下单时间起（可选，格式 YYYY-MM-DD）
     * @param end    下单时间止（可选，格式 YYYY-MM-DD）
     * @return 订单 VO 列表
     */
    List<OrderVO> listOrders(Long userId, String status, String start, String end);

    /**
     * 根据订单ID删除订单（管理后台）
     *
     * @param id 订单ID
     * @return 影响行数
     */
    int deleteOrder(Long id);

    /**
     * 订单发货（管理后台）
     * 由订单状态机执行 PAID → SHIPPED 流转，订单不存在或状态不允许时返回 0
     *
     * @param orderId 订单ID
     * @return 1=发货成功，0=失败（订单不存在或状态不允许）
     */
    int deliverOrder(Long orderId);

    /**
     * 取消订单（仅 DB 状态流转，库存恢复由 Application 层在事务提交后编排）
     *
     * @param orderId 订单ID
     * @return 取消成功返回订单号，否则返回 null（订单不存在或状态不允许取消）
     */
    String cancelOrder(Long orderId);

    /**
     * 处理订单支付成功（状态流转 + DB 库存扣减），状态机天然幂等
     *
     * @param orderNo 订单号
     * @return 本次是否实际完成状态流转；重复回调/状态不允许时返回 false
     */
    boolean paySuccess(String orderNo);

    /**
     * 根据订单号查询订单详情
     * 状态统一输出领域 code 口径，存储值无法识别时兜底 INIT
     *
     * @param orderNo 订单号
     * @return 订单 VO，不存在时返回 null
     */
    OrderVO getOrderByNo(String orderNo);

    /**
     * 继续支付订单
     * 根据订单号重新生成支付链接，用于未支付订单的继续支付场景
     *
     * @param orderNo 订单号
     * @return 订单创建结果（包含支付链接）
     */
    OrderCreateVO continuePay(String orderNo);

    /**
     * 检查订单商品库存
     * 用于继续支付前的库存同步校验
     *
     * @param orderNo 订单号
     * @return true=库存充足，false=库存不足
     */
    boolean checkOrderStock(String orderNo);
}