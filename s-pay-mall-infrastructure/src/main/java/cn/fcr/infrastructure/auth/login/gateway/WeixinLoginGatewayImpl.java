package cn.fcr.infrastructure.auth.login.gateway;

import cn.fcr.domain.auth.login.gateway.IWechatLoginGateway;
import cn.fcr.infrastructure.dao.auth.IUserBindingDao;
import cn.fcr.infrastructure.dao.auth.po.UserBinding;
import cn.fcr.types.common.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * 微信登录网关实现（用户绑定查询与Token缓存）
 *
 * <p>【P0-6】自动注册建户逻辑已上收至 Domain 层
 * {@code IMallUserService.registerWeChatUserByScan}，本网关只保留
 * 查询与缓存两类技术适配，不再直写用户/角色 DAO。</p>
 *
 * @author 傅崇睿
 */
@Slf4j
@Component
public class WeixinLoginGatewayImpl implements IWechatLoginGateway {

    @Resource
    private IUserBindingDao userBindingDao;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Long findUserIdByOpenid(String openid) {
        UserBinding binding = userBindingDao.findByIdentityTypeAndIdentifier(
                Constants.IDENTITY_TYPE_WECHAT_MP, openid);
        return binding != null ? binding.getUserId() : null;
    }

    @Override
    public void saveLoginToken(String ticket, String token) {
        // 缓存登录token，有效期5分钟
        stringRedisTemplate.opsForValue().set(ticket, token, 5, TimeUnit.MINUTES);
    }

    @Override
    public String getLoginToken(String ticket) {
        String token = stringRedisTemplate.opsForValue().get(ticket);
        if (token != null) {
            // 获取后立即删除，保证token一次性使用
            stringRedisTemplate.delete(ticket);
        }
        return token;
    }
}
