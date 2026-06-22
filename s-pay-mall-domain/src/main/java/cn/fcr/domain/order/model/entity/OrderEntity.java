package cn.fcr.domain.order.model.entity;

import cn.fcr.domain.order.model.valobj.OrderStatusVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter; // 替换 @Data，拒绝外界盲目 Setter
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 订单实体，封装订单生命周期状态机与金额校验等核心业务规则。
 *
 * @author 傅崇睿
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderEntity {

    /** 商品 ID */
    private String productId;

    /** 商品名称 */
    private String productName;

    /** 订单号（唯一标识） */
    private String orderId;

    /** 订单创建时间 */
    private Date orderTime;

    /** 订单总金额 */
    private BigDecimal totalAmount;

    /** 订单状态值对象 */
    private OrderStatusVO orderStatusVO;

    /** 支付链接（支付宝/微信支付） */
    private String payUrl;

    /* ==========================================
     * 状态机守卫变迁逻辑 (防御性状态控制)
     * ========================================== */

    /**
     * 检查是否可以支付
     * @return true=可以支付，false=不可以支付
     */
    public boolean canPay() {
        return this.orderStatusVO == OrderStatusVO.CREATE
            || this.orderStatusVO == OrderStatusVO.PAY_WAIT;
    }

    /**
     * 检查是否可以取消订单
     * @return true=可以取消，false=不可以取消
     */
    public boolean canCancel() {
        return this.orderStatusVO == OrderStatusVO.CREATE
            || this.orderStatusVO == OrderStatusVO.PAY_WAIT;
    }

    /**
     * 检查是否可以发货
     * @return true=可以发货，false=不可以发货
     */
    public boolean canDeliver() {
        return this.orderStatusVO == OrderStatusVO.PAY_SUCCESS;
    }

    /**
     * 检查是否可以完成交易
     * @return true=可以完成，false=不可以完成
     */
    public boolean canComplete() {
        return this.orderStatusVO == OrderStatusVO.DEAL_DONE;
    }

    /* ==========================================
     * 状态变迁方法
     * ========================================== */

    /**
     * 标记订单为等待支付状态
     * @throws IllegalStateException 当前状态不允许此操作
     */
    public void markWaitPay() {
        if (this.orderStatusVO != OrderStatusVO.CREATE) {
            throw new IllegalStateException(
                "订单状态为 [" + getSafeStateDesc() + "]，无法标记为等待支付"
            );
        }
        this.orderStatusVO = OrderStatusVO.PAY_WAIT;
    }

    /**
     * 标记订单支付成功
     * 必须校验当前状态是否为 PAY_WAIT
     * @throws IllegalStateException 当前状态不允许此操作
     */
    public void paySuccess() {
        if (this.orderStatusVO != OrderStatusVO.PAY_WAIT) {
            throw new IllegalStateException(
                "订单状态为 [" + getSafeStateDesc() + "]，无法标记为支付成功"
            );
        }
        this.orderStatusVO = OrderStatusVO.PAY_SUCCESS;
    }

    /**
     * 取消订单
     * 必须校验当前状态是否为 CREATE 或 PAY_WAIT
     * @throws IllegalStateException 当前状态不允许此操作（已支付或已完成的订单不可取消）
     */
    public void cancel() {
        if (!canCancel()) {
            throw new IllegalStateException(
                "订单状态为 [" + getSafeStateDesc() + "]，只有创建中或等待支付的订单可以取消"
            );
        }
        this.orderStatusVO = OrderStatusVO.CLOSE;
    }

    /**
     * 订单发货
     * 必须校验当前状态是否为 PAY_SUCCESS
     * @throws IllegalStateException 当前状态不允许此操作
     */
    public void deliver() {
        if (!canDeliver()) {
            throw new IllegalStateException(
                "订单状态为 [" + getSafeStateDesc() + "]，只有已支付订单才能发货"
            );
        }
        this.orderStatusVO = OrderStatusVO.DEAL_DONE;
    }

    /**
     * 交易完成
     * 必须校验当前状态是否为 DEAL_DONE
     * @throws IllegalStateException 当前状态不允许此操作
     */
    public void complete() {
        if (!canComplete()) {
            throw new IllegalStateException(
                "订单状态为 [" + getSafeStateDesc() + "]，无法标记为交易完成"
            );
        }
        this.orderStatusVO = OrderStatusVO.DEAL_DONE;
    }

    /**
     * 超时关单
     * 必须校验当前状态是否为 CREATE
     * @throws IllegalStateException 当前状态不允许此操作
     */
    public void closeByTimeout() {
        if (this.orderStatusVO != OrderStatusVO.CREATE) {
            throw new IllegalStateException(
                "订单状态为 [" + getSafeStateDesc() + "]，只有创建中的订单才能超时关单"
            );
        }
        this.orderStatusVO = OrderStatusVO.CLOSE;
    }

    /* ==========================================
     * 业务规则方法
     * ========================================== */

    /**
     * 安全获取状态描述（防止 NPE）
     * @return 状态描述，若状态为 null 返回 "未知状态"
     */
    public String getSafeStateDesc() {
        return this.orderStatusVO != null
            ? this.orderStatusVO.getDesc()
            : "未知状态";
    }

    /**
     * 安全获取状态码（防止 NPE）
     * @return 状态码，若状态为 null 返回 "UNKNOWN"
     */
    public String getSafeStateCode() {
        return this.orderStatusVO != null
            ? this.orderStatusVO.getCode()
            : "UNKNOWN";
    }

    /**
     * 验证金额是否有效
     * 使用 BigDecimal 确保精度，使用 compareTo 而非 equals
     * @param expectedAmount 期望的金额
     * @return true=金额匹配，false=金额不匹配
     */
    public boolean validateAmount(BigDecimal expectedAmount) {
        if (expectedAmount == null || this.totalAmount == null) {
            return false;
        }
        // 使用 compareTo 而非 equals，防止精度问题
        return this.totalAmount.compareTo(expectedAmount) == 0;
    }

    /**
     * 检查订单是否已支付
     * @return true=已支付，false=未支付
     */
    public boolean isPaid() {
        return this.orderStatusVO == OrderStatusVO.PAY_SUCCESS
            || this.orderStatusVO == OrderStatusVO.DEAL_DONE;
    }

    /**
     * 检查订单是否已关闭
     * @return true=已关闭，false=未关闭
     */
    public boolean isClosed() {
        return this.orderStatusVO == OrderStatusVO.CLOSE;
    }

    /**
     * 设置支付链接
     * 校验状态和参数
     * @param payUrl 支付链接
     * @throws IllegalStateException 当前状态不允许此操作
     * @throws IllegalArgumentException 支付链接为空
     */
    public void initPayUrl(String payUrl) {
        if (payUrl == null || payUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("支付链接不能为空");
        }
        if (!canPay()) {
            throw new IllegalStateException(
                "订单状态为 [" + getSafeStateDesc() + "]，无法生成支付链接"
            );
        }
        this.payUrl = payUrl;
    }
}
