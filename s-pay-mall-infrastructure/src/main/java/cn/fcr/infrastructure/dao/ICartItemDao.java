package cn.fcr.infrastructure.dao;

import cn.fcr.infrastructure.dao.po.CartItem;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 购物车项数据访问接口
 * 继承 MyBatis-Plus BaseMapper，提供购物车项的 CRUD 操作
 * CartItem 实体对应用户购物车中的单个商品项
 */
@Mapper
public interface ICartItemDao extends BaseMapper<CartItem> {

    @Delete("DELETE FROM cart_item WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Long userId);
}
