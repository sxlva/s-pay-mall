package cn.fcr.trigger.http.assembler;

import cn.fcr.api.vo.UserProfileVO;
import cn.fcr.domain.mall.model.valobj.UserProfile;

/**
 * 用户个人信息装配器
 *
 * @author 傅崇睿
 */
public class UserProfileAssembler {

    /**
     * Domain 层 UserProfile → API 层 UserProfileVO
     *
     * @param profile Domain层用户个人信息
     * @return API层VO，profile为null时返回null
     */
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
