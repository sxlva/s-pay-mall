package cn.fcr.domain.mall.model.valobj;

import cn.fcr.domain.shared.model.vo.PayStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单摘要值对象（上下文边界模型），仅包含 mall 模块需要的支付订单字段，
 * 由 Infrastructure 层 Gateway 适配器从 order 领域转换而来。
 *
 * @author 傅崇睿
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderSummaryVO {

    /**
     * 订单号
     */
    private String orderNo;

    /**
     * 支付链接 / 支付二维码
     */
    private String payUrl;

    /**
     * 订单总金额
     */
    private BigDecimal totalAmount;

    /**
     * 支付状态
     */
    private PayStatus status;

    /**
     * 第三方交易号（支付成功后有值）
     */
    private String tradeNo;

    /**
     * 支付完成时间
     */
    private LocalDateTime payTime;
}
