package cn.fcr.domain.mall.model.valobj;

import cn.fcr.domain.mall.model.entity.OrderState;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderVO {

    private Long id;

    private String orderNo;

    private Long userId;

    private BigDecimal totalAmount;

    private String address;

    private String status;

    private String statusDesc;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer totalCount;

    private List<OrderItemVO> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemVO {
        private Long id;
        private Long productId;
        private String productName;
        private BigDecimal price;
        private Integer quantity;
        private BigDecimal itemAmount;
    }
}