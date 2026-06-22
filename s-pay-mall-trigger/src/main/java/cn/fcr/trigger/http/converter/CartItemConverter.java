package cn.fcr.trigger.http.converter;

import cn.fcr.api.dto.CartItemRespDTO;
import cn.fcr.domain.mall.model.valobj.CartItemVO;
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
     * CartItemVO → CartItemRespDTO
     *
     * @param cartItemVO Domain层购物车商品
     * @return API层购物车商品响应
     */
    @Mapping(target = "price", source = "productPrice")
    CartItemRespDTO toRespDTO(CartItemVO cartItemVO);
}
