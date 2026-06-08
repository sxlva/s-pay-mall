package cn.fcr.domain.mall.adapter.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface IStatisticsRepository {

    BigDecimal sumDailySales(String date);

    Integer countDailyOrders(String date);

    List<Map<String, Object>> getCategoryProductCount();
}