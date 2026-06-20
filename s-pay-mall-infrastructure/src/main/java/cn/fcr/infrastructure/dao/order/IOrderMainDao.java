package cn.fcr.infrastructure.dao.order;

import cn.fcr.infrastructure.dao.order.po.OrderMain;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;

/**
 * 商城订单主表数据访问接口
 * 继承 MyBatis-Plus BaseMapper，提供订单的 CRUD 操作
 * 自定义方法支持销售数据统计（日报表）
 */
@Mapper
public interface IOrderMainDao extends BaseMapper<OrderMain> {

    /**
     * 统计指定日期的日销售额
     * 仅统计已支付（PAID）状态的订单
     * 用于后台管理系统的销售日报表
     *
     * @param date 日期，格式为 YYYY-MM-DD
     * @return 日销售额，若无订单返回 0
     */
    @Select("SELECT COALESCE(SUM(total_amount), 0) FROM order_main WHERE DATE(create_time) = #{date} AND status = 'PAID'")
    BigDecimal sumDailySales(@Param("date") String date);

    /**
     * 统计指定日期的订单数量
     * 包含所有状态的订单，用于后台管理系统的订单统计
     *
     * @param date 日期，格式为 YYYY-MM-DD
     * @return 订单数量
     */
    @Select("SELECT COUNT(*) FROM order_main WHERE DATE(create_time) = #{date}")
    Integer countDailyOrders(@Param("date") String date);
}
