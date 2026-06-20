package cn.fcr.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 创建订单 / 继续支付响应 DTO
 * 替代原 Controller 中手动构造的 Map<String, Object>
 *
 * <p>字段名使用 camelCase 与前端的 OrderCreateResult 类型对齐：</p>
 * <ul>
 *   <li>orderId 对应 domain 的 orderNo（通过 MapStruct @Mapping 处理）</li>
 *   <li>html 为条件字段（支付跳转 HTML），仅在 payUrl 包含表单时才设值，
 *       Converter 不处理此字段（自动为 null），由 Controller 手动赋值</li>
 * </ul>
 *
 * <p>NOTE: createOrder 和 continuePay 共用此 DTO。
 * 两者返回的都是同一份 OrderCreateVO 数据，前端均只读取 payUrl 字段，
 * 不依赖 orderNo/orderId 的具体值。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreateRespDTO {

    /** 订单号（由 domain 的 orderNo 映射而来） */
    private String orderId;

    /** 支付 URL */
    private String payUrl;

    /** 订单总金额 */
    private BigDecimal totalAmount;

    /** 支付跳转 HTML 表单（条件字段，可空） */
    private String html;
}
