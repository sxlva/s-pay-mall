package cn.fcr.infrastructure.dao.order;

import cn.fcr.infrastructure.dao.order.po.OrderItem;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单详情数据访问接口（MyBatis-Plus BaseMapper）
 *
 * @author 傅崇睿
 */
@Mapper
public interface IOrderItemDao extends BaseMapper<OrderItem> {
}
