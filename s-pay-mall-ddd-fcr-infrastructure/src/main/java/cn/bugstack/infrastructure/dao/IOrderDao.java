package cn.bugstack.infrastructure.dao;

import cn.bugstack.infrastructure.dao.po.PayOrder;
import org.apache.ibatis.annotations.Mapper;
/**
 * @author xiaolv
 * @date 2025/7/20 18:50
 * @description
 */
@Mapper
public interface IOrderDao {

    void insert(PayOrder payOrder);


    PayOrder queryUnPayOrder(PayOrder payOrderReq);
}
