package cn.fcr.infrastructure.dao;

import cn.fcr.infrastructure.dao.po.PayOrder;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 支付订单数据访问接口
 * 专注于支付流程中的订单管理，包括：
 * - 支付中订单的创建和查询
 * - 支付结果的更新
 * - 超时未支付订单的查询（用于关闭订单）
 * - 支付成功但未回调的订单查询（用于补推回调）
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
     * 查询用户未支付订单
     * 根据用户ID和商品ID查询最近一条未支付订单
     * 用于判断是否需要创建新订单或继续支付
     *
     * @param payOrderReq 查询条件，包含 userId 和 productId
     * @return 未支付订单对象，若不存在返回 null
     */
    @Select("select product_id, product_name, order_id, order_time, total_amount, status, pay_url " +
            "from pay_order where user_id = #{userId} and product_id = #{productId} order by id desc limit 1")
    PayOrder queryUnPayOrder(PayOrder payOrderReq);

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
     * 查询超时未支付订单
     * 查找创建时间超过30分钟但仍为 WAIT_PAY 状态的订单
     * 用于定时任务关闭超期未支付的订单
     *
     * @return 超时未支付的订单ID列表
     */
    @Select("select order_id from pay_order where status = 'WAIT_PAY' and TIMESTAMPDIFF(MINUTE, create_time, NOW()) > 30")
    List<String> queryTimeoutCloseOrderList();

    /**
     * 查询已支付但未成功回调的订单
     * 查找状态为 PAID 的订单（用于补推回调）
     *
     * @return 需要补推回调的订单ID列表
     */
    @Select("select order_id from pay_order where status = 'PAID'")
    List<String> queryNoPayNotifyOrder();

    /**
     * 关闭订单
     * 将订单状态修改为 CLOSE，并记录关闭时间
     * 用于超时自动关闭或用户主动取消
     *
     * @param orderId 订单ID
     */
    @Update("update pay_order set status = 'CLOSED', pay_time = now(), update_time = now() where order_id = #{orderId}")
    void changeOrderClose(@Param("orderId") String orderId);

    /**
     * 查询订单状态
     *
     * @param orderNo 订单号
     * @return 订单状态，如果订单不存在返回null
     */
    @Select("select status from pay_order where order_id = #{orderNo}")
    String queryOrderStatus(@Param("orderNo") String orderNo);

    /**
     * 根据订单号查询订单列表
     *
     * @param orderNo 订单号
     * @return 订单列表
     */
    @Select("select product_id, product_name, order_id, total_amount, status from pay_order where order_id = #{orderNo}")
    List<PayOrder> queryOrderByOrderNo(@Param("orderNo") String orderNo);

    /**
     * 乐观锁方式关闭订单
     *
     * @param orderNo      订单号
     * @param expectStatus 期望状态
     * @return 影响行数
     */
    @Update("update pay_order set status = 'CLOSED', pay_time = now(), update_time = now() where order_id = #{orderNo} and status = #{expectStatus}")
    int closeOrderWithOptimisticLock(@Param("orderNo") String orderNo, @Param("expectStatus") String expectStatus);
}
