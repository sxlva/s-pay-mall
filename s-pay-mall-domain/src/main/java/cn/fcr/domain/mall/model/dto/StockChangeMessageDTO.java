package cn.fcr.domain.mall.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 库存变更消息 DTO
 * 用于 RocketMQ 消息传递，同步 Redis 库存
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockChangeMessageDTO {

    /**
     * 商品ID
     */
    private Long productId;

    /**
     * 新库存值（全量更新）
     */
    private Integer newStock;

    /**
     * 变更类型
     * ADMIN_UPDATE - 后台管理员修改库存
     * PAY_DEDUCT - 支付成功扣减库存
     * ORDER_RESTORE - 订单取消恢复库存
     */
    private String changeType;

    /**
     * 消息唯一ID（用于幂等性检查）
     * 格式建议：{changeType}:{productId}:{timestamp}
     */
    private String messageId;

    /**
     * 消息时间戳（毫秒）
     */
    private Long timestamp;

    /**
     * 变更类型常量
     */
    public static final String CHANGE_TYPE_ADMIN_UPDATE = "ADMIN_UPDATE";
    public static final String CHANGE_TYPE_PAY_DEDUCT = "PAY_DEDUCT";
    public static final String CHANGE_TYPE_ORDER_RESTORE = "ORDER_RESTORE";
}