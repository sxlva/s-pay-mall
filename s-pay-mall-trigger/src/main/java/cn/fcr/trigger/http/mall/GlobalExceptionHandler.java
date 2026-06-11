package cn.fcr.trigger.http.mall;

import cn.fcr.api.response.Response;
import cn.fcr.types.common.Constants;
import cn.fcr.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLIntegrityConstraintViolationException;

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

    @ExceptionHandler(SQLIntegrityConstraintViolationException.class)
    public Response<String> onSQLIntegrityConstraintViolationException(SQLIntegrityConstraintViolationException e) {
        log.error("数据库约束异常", e);
        String message = e.getMessage();
        String friendlyMessage = "操作失败";
        if (message != null) {
            if (message.contains("order_item") || message.contains("product_id")) {
                friendlyMessage = "该商品下仍有关联订单，无法删除！";
            } else if (message.contains("product") || message.contains("category_id")) {
                friendlyMessage = "该分类下仍有关联商品，无法删除！";
            } else if (message.contains("foreign key")) {
                friendlyMessage = "存在关联数据，无法删除！";
            }
        }
        return Response.<String>builder()
                .code(Constants.ResponseCode.UN_ERROR.getCode())
                .info(friendlyMessage)
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
