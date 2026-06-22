package cn.fcr.trigger.http;

import cn.fcr.api.response.Response;
import cn.fcr.types.common.Constants;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.Map;

/**
 * 控制器公共基类
 *
 * @author 傅崇睿
 */
public class BaseController {

    /** JWT签名密钥 */
    @Value("${security.jwt.secret:REPLACED_DEFAULT_JWT_SECRET}")
    private String jwtSecret;

    /**
     * 构建成功响应
     *
     * @param data 响应数据
     * @param <T> 数据类型
     * @return Response 成功响应对象
     */
    protected <T> Response<T> success(T data) {
        return Response.<T>builder()
                .code(Constants.ResponseCode.SUCCESS.getCode())
                .info(Constants.ResponseCode.SUCCESS.getInfo())
                .data(data)
                .build();
    }

    /**
     * 构建失败响应
     *
     * @param info 错误信息
     * @param <T>  响应数据类型
     * @return Response 失败响应对象
     */
    protected <T> Response<T> fail(String info) {
        return Response.<T>builder()
                .code(Constants.ResponseCode.UN_ERROR.getCode())
                .info(info == null || info.isBlank() ? Constants.ResponseCode.UN_ERROR.getInfo() : info)
                .data(null)
                .build();
    }

    /**
     * 从请求头解析当前用户ID
     *
     * <p>从Authorization头中提取JWT token，解析出用户ID。
     * 如果token无效或不存在，抛出IllegalArgumentException。</p>
     *
     * @param request HTTP请求对象
     * @return 当前用户ID
     * @throws IllegalArgumentException token不存在或格式不正确
     */
    protected Long currentUserId(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            throw new IllegalArgumentException("未登录");
        }
        Claims claims = Jwts.parser()
                .setSigningKey(jwtSecret.getBytes())
                .parseClaimsJws(auth.substring(7))
                .getBody();
        return ((Number) claims.get("uid")).longValue();
    }

    /**
     * 将Object安全转为BigDecimal
     *
     * @param value 原始值，可为null
     * @return 转换后的BigDecimal，null返回BigDecimal.ZERO
     */
    protected BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value instanceof Double) {
            return BigDecimal.valueOf((Double) value);
        }
        if (value instanceof Number) {
            return BigDecimal.valueOf(((Number) value).doubleValue());
        }
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    /**
     * 将Object转为Map
     *
     * @param obj 待转换对象
     * @return Map形式的数据
     */
    @SuppressWarnings("unchecked")
    protected Map<String, Object> toMap(Object obj) {
        return new com.fasterxml.jackson.databind.ObjectMapper().convertValue(obj, Map.class);
    }
}
