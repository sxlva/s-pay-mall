package cn.fcr.api;

import cn.fcr.api.dto.CreatePayRequestDTO;
import cn.fcr.api.response.Response;

/**
 * 支付外观接口
 * <p>
 * 对外暴露支付相关的操作接口，提供订单支付能力
 */
public interface IPayFacade {

    /**
     * 创建支付订单
     * <p>
     * 根据订单信息创建支付订单，生成支付链接或支付参数
     *
     * @param createPayRequestDTO 支付请求DTO，包含订单号、金额等支付相关信息
     * @return Response&lt;String&gt; 返回支付链接或支付参数
     */
    Response<String> createPayOrder(CreatePayRequestDTO createPayRequestDTO);

}