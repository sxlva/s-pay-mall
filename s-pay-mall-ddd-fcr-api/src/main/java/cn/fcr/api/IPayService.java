package cn.fcr.api;

import cn.fcr.api.dto.CreatePayRequestDTO;
import cn.fcr.api.response.Response;

/**
 * @author xiaolv
 * @date 2025/8/1 08:07
 * @description
 */
public interface IPayService {
    Response<String> createPayOrder(CreatePayRequestDTO createPayRequestDTO);
}
