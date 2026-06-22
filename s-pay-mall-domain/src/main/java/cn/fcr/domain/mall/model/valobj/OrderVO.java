package cn.fcr.domain.mall.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单视图对象，承载订单详情及其商品项列表。
 *
 * @author 傅崇睿
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderVO {

    /** 订单ID */
    private Long id;

    /** 订单号 */
    private String orderNo;

    /** 用户ID */
    private Long userId;

    /** 订单总金额 */
    private BigDecimal totalAmount;

    /** 收货地址 */
    private String address;

    /** 订单状态码 */
    private String status;

    /** 订单状态描述 */
    private String statusDesc;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 商品总数量 */
    private Integer totalCount;

    /** 订单商品项列表 */
    private List<OrderItemVO> items;

    /**
     * 订单商品项视图对象
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemVO {
        /** 订单项ID */
        private Long id;
        /** 商品ID */
        private Long productId;
        /** 商品名称 */
        private String productName;
        /** 商品单价 */
        private BigDecimal price;
        /** 购买数量 */
        private Integer quantity;
        /** 该项总金额 */
        private BigDecimal itemAmount;
    }
}