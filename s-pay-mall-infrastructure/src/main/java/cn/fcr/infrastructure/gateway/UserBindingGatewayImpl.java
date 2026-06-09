package cn.fcr.infrastructure.gateway;

import cn.fcr.domain.mall.gateway.IUserBindingGateway;
import cn.fcr.infrastructure.dao.IUserBindingDao;
import cn.fcr.infrastructure.dao.po.UserBindingPO;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 用户绑定网关实现
 */
@Component
public class UserBindingGatewayImpl implements IUserBindingGateway {

    @Resource
    private IUserBindingDao userBindingDao;

    private static final String WECHAT_IDENTITY_TYPE = "weixin";

    @Override
    public String getWeChatOpenIdByUserId(Long userId) {
        UserBindingPO binding = userBindingDao.findByUserIdAndIdentityType(userId, WECHAT_IDENTITY_TYPE);
        return binding != null ? binding.getIdentifier() : null;
    }
}