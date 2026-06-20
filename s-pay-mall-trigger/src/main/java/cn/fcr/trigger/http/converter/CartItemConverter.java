package cn.fcr.trigger.http.converter;

import cn.fcr.api.dto.CartItemRespDTO;
import cn.fcr.domain.mall.model.valobj.CartItemVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * 购物车商品 Domain VO → API RespDTO 转换器
 */
@Mapper
public interface CartItemConverter {

    CartItemConverter INSTANCE = Mappers.getMapper(CartItemConverter.class);

    @Mapping(target = "price", source = "productPrice")
    CartItemRespDTO toRespDTO(CartItemVO cartItemVO);
}
