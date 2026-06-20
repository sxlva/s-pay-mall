package cn.fcr.trigger.http.converter;

import cn.fcr.api.dto.OrderCreateRespDTO;
import cn.fcr.domain.mall.model.valobj.OrderCreateVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * 创建订单 / 继续支付 Domain VO → API RespDTO 转换器
 *
 * <p>注意：html 字段不在 Domain VO 中，需在 Controller 中根据条件手动赋值，
 * Converter 对此字段不做处理（自动为 null）。</p>
 */
@Mapper
public interface OrderConverter {

    OrderConverter INSTANCE = Mappers.getMapper(OrderConverter.class);

    @Mapping(target = "orderId", source = "orderNo")
    OrderCreateRespDTO toCreateResp(OrderCreateVO orderCreateVO);
}
