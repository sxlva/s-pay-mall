package cn.fcr.infrastructure.adapter.service;

import cn.fcr.domain.mall.service.IMallStatisticsService;
import cn.fcr.infrastructure.dao.ICategoryDao;
import cn.fcr.infrastructure.dao.IOrderMainDao;
import cn.fcr.infrastructure.dao.IProductDao;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class MallStatisticsServiceImpl implements IMallStatisticsService {

    @Resource
    private IOrderMainDao orderMainDao;

    @Resource
    private IProductDao productDao;

    @Resource
    private ICategoryDao categoryDao;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public List<Map<String, Object>> getSalesTrend() {
        List<Map<String, Object>> result = new ArrayList<>();

        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            String dateStr = date.format(DATE_FORMATTER);

            Map<String, Object> dayData = new HashMap<>();
            dayData.put("date", dateStr);
            dayData.put("dayOfWeek", getDayOfWeek(i));

            BigDecimal totalAmount = orderMainDao.sumDailySales(dateStr);
            dayData.put("salesAmount", totalAmount != null ? totalAmount.doubleValue() : 0);

            Integer orderCount = orderMainDao.countDailyOrders(dateStr);
            dayData.put("orderCount", orderCount != null ? orderCount : 0);

            result.add(dayData);
        }

        return result;
    }

    @Override
    public List<Map<String, Object>> getCategoryRatio() {
        return categoryDao.getCategoryProductCount();
    }

    private String getDayOfWeek(int daysAgo) {
        String[] days = {"今天", "昨天", "前天", "周三", "周二", "周一", "周日"};
        if (daysAgo >= 0 && daysAgo < days.length) {
            return days[daysAgo];
        }
        return LocalDate.now().minusDays(daysAgo).getDayOfWeek().toString();
    }
}