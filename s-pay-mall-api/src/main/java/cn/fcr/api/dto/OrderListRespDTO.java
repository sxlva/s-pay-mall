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
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderListRespDTO {

    private Long id;
    private String orderNo;
    private String status;
    private String statusDesc;
    private BigDecimal totalAmount;
    private LocalDateTime createTime;
    private String address;
}
