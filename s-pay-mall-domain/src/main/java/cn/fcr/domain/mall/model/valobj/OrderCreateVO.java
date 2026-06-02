package cn.fcr.domain.mall.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 订单创建返回值对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreateVO {
    
    private String orderNo;
    
    private BigDecimal totalAmount;
    
    private String status;
    
    private String payUrl;
}
