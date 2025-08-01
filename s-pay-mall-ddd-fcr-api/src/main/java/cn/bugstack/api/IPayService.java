package cn.bugstack.api;

import cn.bugstack.api.dto.CreatePayRequestDTO;
import cn.bugstack.api.response.Response;

/**
 * @author xiaolv
 * @date 2025/8/1 08:07
 * @description
 */
public interface IPayService {
    Response<String> createPayOrder(CreatePayRequestDTO createPayRequestDTO);
}
