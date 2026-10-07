package cn.fcr.infrastructure.dao.mall;

import cn.fcr.infrastructure.dao.mall.po.CartItem;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 购物车项数据访问接口（MyBatis-Plus BaseMapper）
 *
 * @author 傅崇睿
 */
@Mapper
public interface ICartItemDao extends BaseMapper<CartItem> {

    /**
     * 根据用户ID清空购物车
     * 在用户下单成功或删除用户时级联调用
     *
     * @param userId 用户ID
     * @return 影响行数
     */
    @Delete("DELETE FROM cart_item WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Long userId);
}
