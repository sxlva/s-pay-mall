package cn.fcr.trigger.http.converter;

import cn.fcr.api.dto.OrderListRespDTO;
import cn.fcr.domain.mall.model.valobj.OrderVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * 订单列表 Domain VO → API RespDTO 转换器
 *
 * <p>字段名一一对应，无需额外 @Mapping。</p>
 */
@Mapper
public interface OrderListConverter {

    OrderListConverter INSTANCE = Mappers.getMapper(OrderListConverter.class);

    OrderListRespDTO toRespDTO(OrderVO orderVO);
}
