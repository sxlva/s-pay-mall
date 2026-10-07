package cn.fcr.trigger.http.mall;

import cn.fcr.api.response.Response;
import cn.fcr.domain.mall.product.model.exception.CategoryHasProductsException;
import cn.fcr.domain.mall.product.model.exception.ProductHasOrdersException;
import cn.fcr.types.common.Constants;
import cn.fcr.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
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
     * 参数校验失败处理（@Valid 触发）
     *
     * <p>仅处理 JSR-303 参数校验失败，返回契约约定的 0002 非法参数。
     * 刻意不处理 IllegalArgumentException 等业务/程序异常，避免程序 bug
     * 被伪装成用户输入问题（参见 SECURITY_ISSUES S-03）。</p>
     *
     * @param e 参数校验异常
     * @return 错误码 0002，info 为首个字段校验错误提示
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Response<String> onMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String info = (fieldError != null && fieldError.getDefaultMessage() != null)
                ? fieldError.getDefaultMessage()
                : Constants.ResponseCode.ILLEGAL_PARAMETER.getInfo();
        log.warn("参数校验失败: {}", e.getBindingResult().getFieldErrors());
        return Response.<String>builder()
                .code(Constants.ResponseCode.ILLEGAL_PARAMETER.getCode())
                .info(info)
                .build();
    }

    /**
     * JWT 异常处理（S-03：恢复认证语义）
     *
     * <p>token 过期、签名错误、格式非法等统一归 0003 未登录并返回固定文案，
     * 不再落入兜底 Exception 处理器被归为 0001，也不向前端透传 JWT 内部细节
     * （如过期时间戳）。正常路径 token 已在 JwtAuthenticationFilter 校验，
     * 此处为 Controller 内 currentUserId 二次解析的兜底。</p>
     *
     * @param e JWT 异常
     * @return 错误码 0003，固定提示文案
     */
    @ExceptionHandler(io.jsonwebtoken.JwtException.class)
    public Response<String> onJwtException(io.jsonwebtoken.JwtException e) {
        log.warn("JWT 解析异常: {}", e.getMessage());
        return Response.<String>builder()
                .code(Constants.ResponseCode.NO_LOGIN.getCode())
                .info("登录状态无效或已过期，请重新登录")
                .build();
    }

    /**
     * 未知异常兜底处理
     *
     * <p>S-03：禁止向客户端透传原始异常 message（可能包含 NPE 栈顶、SQL、
     * JWT 内部字段等实现细节），统一返回固定文案，原始异常仅落服务端日志。</p>
     *
     * @param e 异常
     * @return 错误响应
     */
    @ExceptionHandler(Exception.class)
    public Response<String> onException(Exception e) {
        log.error("请求失败", e);
        return Response.<String>builder()
                .code(Constants.ResponseCode.UN_ERROR.getCode())
                .info(Constants.ResponseCode.UN_ERROR.getInfo())
                .build();
    }
}
