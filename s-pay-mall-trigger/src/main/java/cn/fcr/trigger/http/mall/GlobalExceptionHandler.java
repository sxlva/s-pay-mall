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
 * 全局异常处理器
 *
 * <p>统一拦截 Controller 层抛出的异常，返回前端可识别的 Response 格式。
 * 包含业务异常、数据库约束异常及未知异常的兜底处理。</p>
 *
 * @author 傅崇睿
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 业务异常处理
     *
     * @param e 业务异常
     * @return 包含错误码和错误信息的响应
     */
    @ExceptionHandler(AppException.class)
    public Response<String> onAppException(AppException e) {
        log.error("业务异常: code={}, info={}", e.getCode(), e.getInfo());
        return Response.<String>builder()
                .code(e.getCode())
                .info(e.getInfo() != null ? e.getInfo() : e.getMessage())
                .build();
    }

    /**
     * 分类下有关联商品时禁止删除
     *
     * @param e 异常
     * @return 错误响应
     */
    @ExceptionHandler(CategoryHasProductsException.class)
    public Response<String> onCategoryHasProductsException(CategoryHasProductsException e) {
        log.warn("分类删除被拒: {}", e.getMessage());
        return Response.<String>builder()
                .code(Constants.ResponseCode.UN_ERROR.getCode())
                .info(e.getMessage())
                .build();
    }

    /**
     * 商品有关联订单时禁止删除
     *
     * @param e 异常
     * @return 错误响应
     */
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
     *
     * <p>正常路径下约束异常已被 Domain Service 前置校验或 Infrastructure 层捕获转换，
     * 此处理器仅处理未预期的数据库约束冲突，返回通用错误信息。</p>
     *
     * @param e 异常
     * @return 错误响应
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public Response<String> onDataIntegrityViolationException(DataIntegrityViolationException e) {
        log.error("未预期的数据库约束异常", e);
        return Response.<String>builder()
                .code(Constants.ResponseCode.UN_ERROR.getCode())
                .info("操作失败，存在关联数据无法删除")
                .build();
    }

    /**
     * 未知异常兜底处理
     *
     * @param e 异常
     * @return 错误响应
     */
    @ExceptionHandler(Exception.class)
    public Response<String> onException(Exception e) {
        log.error("请求失败", e);
        return Response.<String>builder()
                .code(Constants.ResponseCode.UN_ERROR.getCode())
                .info(e.getMessage() == null ? Constants.ResponseCode.UN_ERROR.getInfo() : e.getMessage())
                .build();
    }
}
