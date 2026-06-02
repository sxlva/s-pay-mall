package cn.fcr.trigger.http.mall;

import cn.fcr.api.response.Response;
import cn.fcr.types.common.Constants;
import cn.fcr.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * @author fcr
 * @description 全局异常处理器，统一返回前端可识别的错误响应
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AppException.class)
    public Response<String> onAppException(AppException e) {
        log.error("业务异常: code={}, info={}", e.getCode(), e.getInfo());
        return Response.<String>builder()
                .code(e.getCode())
                .info(e.getInfo() != null ? e.getInfo() : e.getMessage())
                .build();
    }

    @ExceptionHandler(Exception.class)
    public Response<String> onException(Exception e) {
        log.error("请求失败", e);
        return Response.<String>builder()
                .code(Constants.ResponseCode.UN_ERROR.getCode())
                .info(e.getMessage() == null ? Constants.ResponseCode.UN_ERROR.getInfo() : e.getMessage())
                .build();
    }
}
