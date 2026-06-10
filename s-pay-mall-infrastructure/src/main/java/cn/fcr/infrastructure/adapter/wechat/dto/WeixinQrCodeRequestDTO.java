package cn.fcr.infrastructure.adapter.wechat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @description 微信二维码请求DTO
 * 
 * 【职责说明】
 * - 封装微信API创建二维码的请求参数
 * - 用于向微信服务器请求生成带参数的二维码
 * 
 * 【字段说明】
 * - expire_seconds: 二维码过期时间（秒），最大2592000（30天）
 * - action_name: 二维码类型，QR_SCENE为临时二维码
 * - action_info: 二维码详细信息，包含scene_id或scene_str
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeixinQrCodeRequestDTO {

    /**
     * 二维码过期时间（秒）
     */
    private Integer expire_seconds;

    /**
     * 二维码类型
     */
    private String action_name;

    /**
     * 二维码详细信息
     */
    private ActionInfo action_info;

    /**
     * 二维码详细信息内部类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActionInfo {

        /**
         * 场景信息
         */
        private Scene scene;

        /**
         * 场景信息内部类
         */
        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Scene {

            /**
             * 场景ID
             */
            private Long scene_id;

            /**
             * 场景字符串（用于字符串场景值）
             */
            private String scene_str;
        }
    }

    /**
     * 二维码类型枚举
     */
    public enum ActionNameTypeVO {
        QR_SCENE("QR_SCENE"),
        QR_STR_SCENE("QR_STR_SCENE"),
        QR_LIMIT_SCENE("QR_LIMIT_SCENE"),
        QR_LIMIT_STR_SCENE("QR_LIMIT_STR_SCENE");

        private final String code;

        ActionNameTypeVO(String code) {
            this.code = code;
        }

        public String getCode() {
            return code;
        }
    }

}