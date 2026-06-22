package cn.fcr.domain.mall.service;

import cn.fcr.domain.mall.model.entity.OrderEntity;
import cn.fcr.domain.mall.model.valobj.CartItemVO;
import cn.fcr.domain.mall.model.valobj.OrderCreateVO;
import cn.fcr.domain.mall.model.valobj.OrderVO;

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

    List<OrderVO> listOrders(Long userId, String status, String start, String end);

    int deleteOrder(Long id);

    int deliverOrder(Long orderId);

    int cancelOrder(Long orderId);

    void paySuccess(String orderNo);

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