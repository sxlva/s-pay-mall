package cn.fcr.infrastructure.adapter.repository;

import cn.fcr.domain.mall.adapter.repository.IStatisticsRepository;
import cn.fcr.infrastructure.dao.ICategoryDao;
import cn.fcr.infrastructure.dao.IOrderMainDao;
import cn.fcr.infrastructure.dao.IProductDao;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Repository
public class StatisticsRepository implements IStatisticsRepository {

    @Resource
    private IOrderMainDao orderMainDao;

    @Resource
    private IProductDao productDao;

    @Resource
    private ICategoryDao categoryDao;

    @Override
    public BigDecimal sumDailySales(String date) {
        return orderMainDao.sumDailySales(date);
    }

    @Override
    public Integer countDailyOrders(String date) {
        return orderMainDao.countDailyOrders(date);
    }

    @Override
    public List<Map<String, Object>> getCategoryProductCount() {
        return categoryDao.getCategoryProductCount();
    }
}