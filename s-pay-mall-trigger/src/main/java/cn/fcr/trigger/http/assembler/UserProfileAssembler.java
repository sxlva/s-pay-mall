package cn.fcr.trigger.http.assembler;

import cn.fcr.api.vo.UserProfileVO;
import cn.fcr.domain.mall.model.valobj.UserProfile;

/**
 * 用户个人信息装配器
 * <p>
 * 负责 Domain 层 UserProfile → API 层 UserProfileVO 的转换。
 */
public class UserProfileAssembler {

    public static UserProfileVO toVO(UserProfile profile) {
        if (profile == null) {
            return null;
        }
        return UserProfileVO.builder()
                .id(profile.getId())
                .username(profile.getUsername())
                .status(profile.getStatus())
                .roleCode(profile.getRoleCode())
                .createTime(profile.getCreateTime())
                .updateTime(profile.getUpdateTime())
                .build();
    }
}
