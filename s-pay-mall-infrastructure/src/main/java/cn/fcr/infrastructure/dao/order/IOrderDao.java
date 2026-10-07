package cn.fcr.infrastructure.dao.order;

import cn.fcr.infrastructure.dao.order.po.PayOrder;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 支付订单数据访问接口
 *
 * @author 傅崇睿
 */
@Mapper
public interface IOrderDao {

    /**
     * 插入支付订单记录
     * 在用户发起支付时创建订单
     *
     * @param payOrder 支付订单对象，包含用户ID、商品ID、订单金额等信息
     */
    @Insert("insert into pay_order(user_id, product_id, product_name, order_id, order_time, total_amount, status, create_time, update_time) " +
            "values(#{userId}, #{productId}, #{productName}, #{orderId}, #{orderTime}, #{totalAmount}, #{status}, now(), now())")
    void insert(PayOrder payOrder);

    /**
     * 根据订单号查询支付订单
     *
     * @param orderNo 订单号
     * @return 支付订单对象，若不存在返回 null
     */
    @Select("select id, user_id, product_id, product_name, order_id, order_time, total_amount, status, pay_url, pay_time, create_time, update_time " +
            "from pay_order where order_id = #{orderNo}")
    PayOrder queryByOrderNo(@Param("orderNo") String orderNo);

    /**
     * 更新订单支付信息
     * 在调用第三方支付接口后，保存支付链接和状态
     *
     * @param payOrderReq 包含 orderId、payUrl、status 的订单对象
     */
    @Update("update pay_order set pay_url = #{payUrl}, status = #{status}, update_time = now() where order_id = #{orderId}")
    void updateOrderPayInfo(PayOrder payOrderReq);

    /**
     * 修改订单为支付成功状态
     * 在收到支付成功回调时调用
     *
     * @param payOrderReq 包含 orderId 和 status 的订单对象
     */
    @Update("update pay_order set status = #{status}, pay_time = now(), update_time = now() where order_id = #{orderId}")
    void changeOrderPaySuccess(PayOrder payOrderReq);

    /**
     * 查询等待支付超过5分钟但未收到支付宝回调的订单（pay_order.status = WAIT_PAY），用于主动补单
     *
     * @return 需要主动补单的订单ID列表
     */
    @Select("select order_id from pay_order where status = 'WAIT_PAY' and create_time < DATE_SUB(NOW(), INTERVAL 5 MINUTE)")
    List<String> queryNoPayNotifyOrder();

    /**
     * 查询等待支付超过40分钟仍未关闭的订单（pay_order.status = WAIT_PAY），用于 Job 兜底关单补偿。
     * 正常链路由下单时发送的 30 分钟延时消息关单（TD-7），本查询以 40 分钟为界，
     * 仅兜住延时消息丢失/消费失败的漏网订单，不与正常关单链路竞争。
     *
     * @return 需要兜底关单的订单号列表
     */
    @Select("select order_id from pay_order where status = 'WAIT_PAY' and create_time < DATE_SUB(NOW(), INTERVAL 40 MINUTE)")
    List<String> queryStaleWaitPayOrders();

    /**
     * 关闭订单
     * 将订单状态修改为 CLOSED，更新更新时间
     * 用于超时自动关闭或用户主动取消；pay_time 仅在支付成功时写入，关闭订单不回写
     *
     * @param orderId 订单ID
     */
    @Update("update pay_order set status = 'CLOSED', update_time = now() where order_id = #{orderId}")
    void changeOrderClose(@Param("orderId") String orderId);

    /**
     * 查询订单状态
     *
     * @param orderNo 订单号
     * @return 订单状态，如果订单不存在返回null
     */
    @Select("select status from pay_order where order_id = #{orderNo}")
    String queryOrderStatus(@Param("orderNo") String orderNo);
}
