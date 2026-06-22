package cn.fcr.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 订单库存检查响应 DTO
 * 替代原 Controller 中手动构造的 Map<String, Object>*
 * 字段名使用 camelCase，与前端的 StockCheckResult 类型对齐。
 *
 * @author 傅崇睿
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockCheckRespDTO {

    /** 库存检查是否通过 */
    private Boolean success;

    /** 检查结果消息 */
    private String message;
}
