package cn.fcr.trigger.http.converter;

import cn.fcr.api.dto.user.res.UserCartItemRes;
import cn.fcr.domain.mall.cart.model.valobj.CartItemVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * 购物车商品 Domain VO → API RespDTO 转换器
 *
 * @author 傅崇睿
 */
@Mapper
public interface CartItemConverter {

    CartItemConverter INSTANCE = Mappers.getMapper(CartItemConverter.class);

    /**
     * CartItemVO → UserCartItemRes
     *
     * @param cartItemVO Domain层购物车商品
     * @return API层购物车商品响应
     */
    @Mapping(target = "price", source = "productPrice")
    UserCartItemRes toRespDTO(CartItemVO cartItemVO);
}
