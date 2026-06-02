package cn.fcr.infrastructure.dao;

import cn.fcr.infrastructure.dao.po.OrderItem;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单详情数据访问接口
 * 继承 MyBatis-Plus BaseMapper，提供订单详情的 CRUD 操作
 * OrderItem 实体对应商城订单中的单个商品项（与 OrderMain 为主从关系）
 */
@Mapper
public interface IOrderItemDao extends BaseMapper<OrderItem> {
}