package cn.fcr.trigger.http.assembler;

import cn.fcr.api.dto.user.res.UserProfileRes;
import cn.fcr.domain.mall.user.model.valobj.UserProfile;

/**
 * 用户个人信息装配器
 *
 * @author 傅崇睿
 */
public class UserProfileAssembler {

    /**
     * Domain 层 UserProfile → API 层 UserProfileRes
     *
     * @param profile Domain层用户个人信息
     * @return API层VO，profile为null时返回null
     */
    public static UserProfileRes toVO(UserProfile profile) {
        if (profile == null) {
            return null;
        }
        return UserProfileRes.builder()
                .id(profile.getId())
                .username(profile.getUsername())
                .status(profile.getStatus())
                .roleCode(profile.getRoleCode())
                .createTime(profile.getCreateTime())
                .updateTime(profile.getUpdateTime())
                .wechatBound(profile.getWechatBound())
                .build();
    }
}
