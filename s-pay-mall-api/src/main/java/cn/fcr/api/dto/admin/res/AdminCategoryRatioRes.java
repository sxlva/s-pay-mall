package cn.fcr.api.dto.admin.res;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 分类销售占比视图对象
 *
 * @author 傅崇睿
 */
@Data
public class AdminCategoryRatioRes {

    /** 分类名称 */
    private String categoryName;

    /** 商品数量 */
    private Integer productCount;

    /** 销售额 */
    private BigDecimal salesAmount;
}
