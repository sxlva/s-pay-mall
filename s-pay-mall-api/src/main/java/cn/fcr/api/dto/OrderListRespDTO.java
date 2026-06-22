package cn.fcr.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单列表响应 DTO
 * 替代原 Controller 中手动构造的 Map<String, Object>
 *
 * 字段名使用 camelCase，与前端 Order 类型对齐。
 * 字段与 domain OrderVO 同名，MapStruct 可直接自动映射。
 *
 * @author 傅崇睿
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderListRespDTO {

    /** 订单主键ID */
    private Long id;

    /** 订单号 */
    private String orderNo;

    /** 订单状态 */
    private String status;

    /** 订单状态描述 */
    private String statusDesc;

    /** 订单总金额 */
    private BigDecimal totalAmount;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 收货地址 */
    private String address;
}
