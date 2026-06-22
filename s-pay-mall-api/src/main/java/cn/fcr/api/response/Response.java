package cn.fcr.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 统一响应包装类
 * <p>
 * 封装 API 返回结果，包含状态码、提示信息和数据体
 *
 * @param <T> 响应数据的具体类型
 * @author 傅崇睿
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Response<T> implements Serializable {

    private static final long serialVersionUID = 7000723935764546321L;

    /** 状态码 */
    private String code;

    /** 提示信息 */
    private String info;

    /** 响应数据 */
    private T data;

}
