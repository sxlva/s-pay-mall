package cn.fcr.domain.mall.service.impl;

import cn.fcr.domain.mall.adapter.repository.IStatisticsRepository;
import cn.fcr.domain.mall.service.IMallStatisticsService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MallStatisticsServiceImpl implements IMallStatisticsService {

    private final IStatisticsRepository statisticsRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public MallStatisticsServiceImpl(IStatisticsRepository statisticsRepository) {
        this.statisticsRepository = statisticsRepository;
    }

    @Override
    public List<Map<String, Object>> getSalesTrend() {
        List<Map<String, Object>> result = new ArrayList<>();

        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            String dateStr = date.format(DATE_FORMATTER);

            Map<String, Object> dayData = new HashMap<>();
            dayData.put("date", dateStr);
            dayData.put("dayOfWeek", getDayOfWeek(i));

            BigDecimal totalAmount = statisticsRepository.sumDailySales(dateStr);
            dayData.put("salesAmount", totalAmount != null ? totalAmount.doubleValue() : 0);

            Integer orderCount = statisticsRepository.countDailyOrders(dateStr);
            dayData.put("orderCount", orderCount != null ? orderCount : 0);

            result.add(dayData);
        }

        return result;
    }

    @Override
    public List<Map<String, Object>> getCategoryRatio() {
        return statisticsRepository.getCategoryProductCount();
    }

    private String getDayOfWeek(int daysAgo) {
        String[] days = {"今天", "昨天", "前天", "周三", "周二", "周一", "周日"};
        if (daysAgo >= 0 && daysAgo < days.length) {
            return days[daysAgo];
        }
        return LocalDate.now().minusDays(daysAgo).getDayOfWeek().toString();
    }
}