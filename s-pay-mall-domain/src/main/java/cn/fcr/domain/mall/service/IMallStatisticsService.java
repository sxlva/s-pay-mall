package cn.fcr.domain.mall.service;

import java.util.List;
import java.util.Map;

/**
 * 统计领域服务接口，定义销售趋势和分类占比查询的抽象。
 *
 * @author 傅崇睿
 */
public interface IMallStatisticsService {
    
    /**
     * 获取最近7天销售额趋势
     * @return 包含日期、销售额、订单数的列表
     */
    List<Map<String, Object>> getSalesTrend();
    
    /**
     * 获取商品分类占比
     * @return 包含分类名称和商品数量的列表
     */
    List<Map<String, Object>> getCategoryRatio();
}