package cn.fcr.domain.shared.model.entity;

import cn.fcr.domain.shared.model.vo.PayStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 支付订单实体，定义在 shared 模块中供 order 和 mall 领域共同使用，
 * 代表一笔支付交易的完整信息，包含支付链接、第三方交易号和支付状态。
 *
 * @author 傅崇睿
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PayOrderEntity {

    /** 主键ID */
    private Long id;
    /** 订单号 */
    private String orderNo;
    /** 用户ID */
    private String userId;
    /** 商品ID（多个以逗号分隔） */
    private String productId;
    /** 商品名称（多个以顿号分隔） */
    private String productName;
    /** 订单总金额 */
    private BigDecimal totalAmount;
    /** 支付状态 */
    private PayStatus status;
    /** 支付链接URL */
    private String payUrl;
    /** 第三方交易流水号 */
    private String tradeNo;
    /** 支付完成时间 */
    private LocalDateTime payTime;
    /** 创建时间 */
    private LocalDateTime createTime;
    /** 更新时间 */
    private LocalDateTime updateTime;

    public boolean canPay() {
        return this.status == PayStatus.WAIT_PAY;
    }

    public boolean canClose() {
        return this.status == PayStatus.WAIT_PAY || this.status == PayStatus.PAYING;
    }

    public void initPayUrl(String payUrl) {
        if (this.status != PayStatus.WAIT_PAY) {
            throw new IllegalStateException("订单当前状态为 [" + status.getDescription() + "]，无法生成支付链接");
        }
        if (payUrl == null || payUrl.isBlank()) {
            throw new IllegalArgumentException("支付链接不能为空");
        }
        this.payUrl = payUrl;
        this.status = PayStatus.PAYING;
        this.updateTime = LocalDateTime.now();
    }

    public void verifyCallbackSign(Map<String, String> callbackParams, String expectedOrderNo, BigDecimal expectedAmount) {
        if (callbackParams == null || callbackParams.isEmpty()) {
            throw new IllegalArgumentException("回调参数不能为空");
        }

        String callbackOrderNo = callbackParams.get("out_trade_no");
        String callbackAmount = callbackParams.get("total_amount");
        String tradeStatus = callbackParams.get("trade_status");

        if (!expectedOrderNo.equals(callbackOrderNo)) {
            throw new IllegalStateException("回调订单号不匹配：期望[" + expectedOrderNo + "]，实际[" + callbackOrderNo + "]");
        }

        if (expectedAmount.compareTo(new BigDecimal(callbackAmount)) != 0) {
            throw new IllegalStateException("回调金额不匹配：期望[" + expectedAmount + "]，实际[" + callbackAmount + "]");
        }

        if (!"TRADE_SUCCESS".equals(tradeStatus) && !"TRADE_FINISHED".equals(tradeStatus)) {
            throw new IllegalStateException("交易状态异常：[ " + tradeStatus + " ]");
        }
    }

    public void completePay(String tradeNo) {
        if (this.status != PayStatus.PAYING && this.status != PayStatus.WAIT_PAY) {
            throw new IllegalStateException("订单当前状态为 [" + status.getDescription() + "]，无法完成支付");
        }
        if (tradeNo == null || tradeNo.isBlank()) {
            throw new IllegalArgumentException("第三方交易号不能为空");
        }
        this.tradeNo = tradeNo;
        this.status = PayStatus.PAID;
        this.payTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
    }

    public void close() {
        if (!canClose()) {
            throw new IllegalStateException("订单当前状态为 [" + status.getDescription() + "]，无法关闭");
        }
        this.status = PayStatus.CLOSED;
        this.updateTime = LocalDateTime.now();
    }

    public void markFailed() {
        if (this.status == PayStatus.PAID) {
            throw new IllegalStateException("订单已支付成功，无法标记为失败");
        }
        this.status = PayStatus.FAILED;
        this.updateTime = LocalDateTime.now();
    }

    public static PayOrderEntity create(String orderNo, String userId, String productId,
                                        String productName, BigDecimal totalAmount) {
        PayOrderEntity entity = new PayOrderEntity();
        entity.setOrderNo(orderNo);
        entity.setUserId(userId);
        entity.setProductId(productId);
        entity.setProductName(productName);
        entity.setTotalAmount(totalAmount);
        entity.setStatus(PayStatus.WAIT_PAY);
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        return entity;
    }
}
