package cn.fcr.domain.mall.statistics.adapter.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 统计仓储接口，定义日销售额、订单量和分类商品数统计的抽象。
 *
 * @author 傅崇睿
 */
public interface IStatisticsRepository {

    /**
     * 统计指定日期的日销售额
     * 仅统计已支付（PAID）状态的订单
     *
     * @param date 日期，格式为 YYYY-MM-DD
     * @return 日销售额，若无已支付订单返回 0
     */
    BigDecimal sumDailySales(String date);

    /**
     * 统计指定日期的订单数量
     * 包含所有状态的订单
     *
     * @param date 日期，格式为 YYYY-MM-DD
     * @return 当日订单数量
     */
    Integer countDailyOrders(String date);

    /**
     * 查询各分类的商品数量统计
     * 用于后台分类占比统计
     *
     * @return 分类名称与商品数量的键值对列表
     */
    List<Map<String, Object>> getCategoryProductCount();
}