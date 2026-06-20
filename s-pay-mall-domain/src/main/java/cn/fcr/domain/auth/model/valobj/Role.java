package cn.fcr.domain.auth.model.valobj;

/**
 * 用户角色枚举
 * <p>
 * 【共享值对象】定义在 auth 域，封装角色编码与中文描述的映射。
 * 避免展示层字符串硬编码或 Domain 实体中混入显示逻辑。
 */
public enum Role {

    ADMIN("ADMIN", "管理员"),
    USER("USER", "普通会员");

    private final String code;
    private final String desc;

    Role(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    /**
     * 获取角色中文描述
     */
    public String getDesc() {
        return desc;
    }

    /**
     * 根据角色编码获取中文描述
     *
     * @param code 角色编码
     * @return 中文描述，若无法识别则返回原 code
     */
    public static String descByCode(String code) {
        if (code == null) return null;
        for (Role r : values()) {
            if (r.code.equals(code)) {
                return r.desc;
            }
        }
        return code;
    }
}
