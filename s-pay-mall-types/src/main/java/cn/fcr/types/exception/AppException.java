package cn.fcr.types.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 应用层业务异常，携带异常码和异常信息
 *
 * @author 傅崇睿
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class AppException extends RuntimeException {

    @java.io.Serial
    private static final long serialVersionUID = 5317680961212299217L;

    /** 异常码 */
    private String code;

    /** 异常信息 */
    private String info;

    /**
     * 仅携带异常码
     *
     * @param code 异常码
     */
    public AppException(String code) {
        this.code = code;
    }

    /**
     * 携带异常码及原始异常
     *
     * @param code  异常码
     * @param cause 原始异常
     */
    public AppException(String code, Throwable cause) {
        this.code = code;
        super.initCause(cause);
    }

    /**
     * 携带异常码及异常信息
     *
     * @param code    异常码
     * @param message 异常信息
     */
    public AppException(String code, String message) {
        this.code = code;
        this.info = message;
    }

    /**
     * 携带异常码、异常信息及原始异常
     *
     * @param code    异常码
     * @param message 异常信息
     * @param cause   原始异常
     */
    public AppException(String code, String message, Throwable cause) {
        this.code = code;
        this.info = message;
        super.initCause(cause);
    }

    @Override
    public String toString() {
        return "cn.fcr.x.api.types.exception.XApiException{" +
                "code='" + code + '\'' +
                ", info='" + info + '\'' +
                '}';
    }

}
