package cn.fcr.domain.mall.adapter.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 统计仓储接口，定义日销售额、订单量和分类商品数统计的抽象。
 *
 * @author 傅崇睿
 */
public interface IStatisticsRepository {

    BigDecimal sumDailySales(String date);

    Integer countDailyOrders(String date);

    List<Map<String, Object>> getCategoryProductCount();
}