package cn.fcr.infrastructure.mall.gateway;

import cn.fcr.domain.mall.gateway.IUserBindingGateway;
import cn.fcr.infrastructure.dao.auth.IUserBindingDao;
import cn.fcr.infrastructure.dao.auth.po.UserBinding;
import cn.fcr.types.common.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;

/**
 * 用户绑定网关实现
 *
 * @author 傅崇睿
 */
@Slf4j
@Component
public class UserBindingGatewayImpl implements IUserBindingGateway {

    @Resource
    private IUserBindingDao userBindingDao;

    @Override
    public String getWeChatOpenIdByUserId(Long userId) {
        UserBinding binding = userBindingDao.findByUserIdAndIdentityType(userId, Constants.IDENTITY_TYPE_WECHAT_MP);
        return binding != null ? binding.getIdentifier() : null;
    }

    @Override
    public boolean isWeChatOpenIdBound(String openId) {
        UserBinding binding = userBindingDao.findByIdentityTypeAndIdentifier(Constants.IDENTITY_TYPE_WECHAT_MP, openId);
        return binding != null;
    }

    @Override
    public void bindWeChatOpenId(Long userId, String openId) {
        UserBinding binding = new UserBinding();
        binding.setUserId(userId);
        binding.setIdentityType(Constants.IDENTITY_TYPE_WECHAT_MP);
        binding.setIdentifier(openId);
        binding.setCreateTime(LocalDateTime.now());
        userBindingDao.insert(binding);
        log.info("绑定微信账号成功: userId={}, openId={}", userId, openId);
    }
}
