package cn.fcr.domain.mall.model.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 商品保存命令对象，封装商品新增或更新的操作指令。
 *
 * @author 傅崇睿
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSaveCommand {

    /**
     * 商品ID（新增时为null，更新时必填）
     */
    private Long id;

    /**
     * 分类ID
     */
    private Long categoryId;

    /**
     * 商品名称
     */
    private String name;

    /**
     * 商品描述
     */
    private String description;

    /**
     * 商品价格
     */
    private BigDecimal price;

    /**
     * 商品库存
     */
    private Integer stock;

    /**
     * 商品状态
     */
    private Integer status;
}