package cn.fcr.domain.mall.statistics.service.impl;

import cn.fcr.domain.mall.statistics.adapter.repository.IStatisticsRepository;
import cn.fcr.domain.mall.statistics.service.IMallStatisticsService;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 统计领域服务实现，负责近7天销售趋势和分类占比的计算。
 *
 * @author 傅崇睿
 */
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
            dayData.put("dayOfWeek", getDayOfWeek(date));

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

    /**
     * 生成展示用星期标签：当天/昨天/前天使用相对称呼，更早的日期按真实星期返回
     *
     * @param date 日期
     * @return 星期标签（今天/昨天/前天/周一~周日）
     */
    private String getDayOfWeek(LocalDate date) {
        long daysAgo = ChronoUnit.DAYS.between(date, LocalDate.now());
        if (daysAgo == 0) {
            return "今天";
        }
        if (daysAgo == 1) {
            return "昨天";
        }
        if (daysAgo == 2) {
            return "前天";
        }
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return switch (dayOfWeek) {
            case MONDAY -> "周一";
            case TUESDAY -> "周二";
            case WEDNESDAY -> "周三";
            case THURSDAY -> "周四";
            case FRIDAY -> "周五";
            case SATURDAY -> "周六";
            case SUNDAY -> "周日";
        };
    }
}