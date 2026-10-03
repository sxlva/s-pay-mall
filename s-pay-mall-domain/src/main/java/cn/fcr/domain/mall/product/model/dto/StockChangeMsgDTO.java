package cn.fcr.domain.mall.product.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 库存变更消息 DTO，用于在策略模式中传递库存变更信息。
 *
 * @author 傅崇睿
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockChangeMsgDTO {

    /**
     * 商品ID
     */
    private Long productId;

    /**
     * 变更数量（扣减时为负数，恢复时为正数）
     */
    private Integer changeQuantity;

    /**
     * 变更后库存（全量更新时使用）
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
     * 消息唯一ID（用于日志追踪）
     */
    private String messageId;

    /**
     * 业务单号（用于幂等性检查）
     * 格式：orderId（扣减/恢复）或 updateRecordId（管理员更新）
     */
    private String businessNo;

    /**
     * 消息时间戳（毫秒）
     */
    private Long timestamp;

    /**
     * 变更类型：后台管理员修改库存
     * 走全量更新语义，使用 {@link #newStock} 直接设置库存
     */
    public static final String CHANGE_TYPE_ADMIN_UPDATE = "ADMIN_UPDATE";

    /**
     * 变更类型：支付成功扣减库存
     * {@link #changeQuantity} 为负数，对应 DeductHandler
     */
    public static final String CHANGE_TYPE_PAY_DEDUCT = "PAY_DEDUCT";

    /**
     * 变更类型：订单取消/超时关单恢复库存
     * {@link #changeQuantity} 为正数，对应 RestoreHandler
     */
    public static final String CHANGE_TYPE_ORDER_RESTORE = "ORDER_RESTORE";
}
