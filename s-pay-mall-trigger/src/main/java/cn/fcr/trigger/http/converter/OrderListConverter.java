package cn.fcr.trigger.http.converter;

import cn.fcr.api.dto.OrderListRespDTO;
import cn.fcr.domain.mall.model.valobj.OrderVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * 订单列表 Domain VO → API RespDTO 转换器
 *
 * <p>字段名一一对应，无需额外 @Mapping。</p>
 *
 * @author 傅崇睿
 */
@Mapper
public interface OrderListConverter {

    OrderListConverter INSTANCE = Mappers.getMapper(OrderListConverter.class);

    /**
     * OrderVO → OrderListRespDTO
     *
     * @param orderVO Domain层订单VO
     * @return API层订单列表响应
     */
    OrderListRespDTO toRespDTO(OrderVO orderVO);
}
