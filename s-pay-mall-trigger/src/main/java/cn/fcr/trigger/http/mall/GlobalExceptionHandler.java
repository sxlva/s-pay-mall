package cn.fcr.trigger.http.mall;

import cn.fcr.api.response.Response;
import cn.fcr.domain.mall.model.exception.CategoryHasProductsException;
import cn.fcr.domain.mall.model.exception.ProductHasOrdersException;
import cn.fcr.types.common.Constants;
import cn.fcr.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
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

    @ExceptionHandler(CategoryHasProductsException.class)
    public Response<String> onCategoryHasProductsException(CategoryHasProductsException e) {
        log.warn("分类删除被拒: {}", e.getMessage());
        return Response.<String>builder()
                .code(Constants.ResponseCode.UN_ERROR.getCode())
                .info(e.getMessage())
                .build();
    }

    @ExceptionHandler(ProductHasOrdersException.class)
    public Response<String> onProductHasOrdersException(ProductHasOrdersException e) {
        log.warn("商品删除被拒: {}", e.getMessage());
        return Response.<String>builder()
                .code(Constants.ResponseCode.UN_ERROR.getCode())
                .info(e.getMessage())
                .build();
    }

    /**
     * 数据库约束异常兜底处理
     * 正常路径下约束异常已被 Domain Service 前置校验或 Infrastructure 层捕获转换，
     * 此处理器仅处理未预期的数据库约束冲突，返回通用错误信息。
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public Response<String> onDataIntegrityViolationException(DataIntegrityViolationException e) {
        log.error("未预期的数据库约束异常", e);
        return Response.<String>builder()
                .code(Constants.ResponseCode.UN_ERROR.getCode())
                .info("操作失败，存在关联数据无法删除")
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
