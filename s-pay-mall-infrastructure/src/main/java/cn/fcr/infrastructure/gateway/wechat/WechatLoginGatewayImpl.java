package cn.fcr.infrastructure.gateway.wechat;

import cn.fcr.domain.auth.gateway.IWechatLoginGateway;
import cn.fcr.infrastructure.dao.IMallUserDao;
import cn.fcr.infrastructure.dao.IUserBindingDao;
import cn.fcr.infrastructure.dao.IUserRoleDao;
import cn.fcr.infrastructure.dao.po.MallUser;
import cn.fcr.infrastructure.dao.po.UserBindingPO;
import cn.fcr.types.common.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class WechatLoginGatewayImpl implements IWechatLoginGateway {

    @Resource
    private IMallUserDao mallUserDao;

    @Resource
    private IUserBindingDao userBindingDao;

    @Resource
    private IUserRoleDao userRoleDao;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private static final Long MEMBER_ROLE_ID = 2L;

    @Override
    public Long findUserIdByOpenid(String openid) {
        UserBindingPO binding = userBindingDao.findByIdentityTypeAndIdentifier(
                Constants.IDENTITY_TYPE_WECHAT_MP, openid);
        return binding != null ? binding.getUserId() : null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createWechatUserAndBind(String openid) {
        log.info("创建微信用户并绑定: openid={}", openid);
        
        String defaultUsername = "wx_" + UUID.randomUUID().toString().substring(0, 8);
        
        MallUser newUser = new MallUser();
        newUser.setUsername(defaultUsername);
        newUser.setPassword("");
        newUser.setStatus(Constants.USER_STATUS_WECHAT);
        newUser.setCreateTime(LocalDateTime.now());
        newUser.setUpdateTime(LocalDateTime.now());
        mallUserDao.insert(newUser);
        
        Long userId = newUser.getId();
        log.info("创建新用户成功: userId={}, username={}", userId, defaultUsername);
        
        UserBindingPO binding = new UserBindingPO();
        binding.setUserId(userId);
        binding.setIdentityType(Constants.IDENTITY_TYPE_WECHAT_MP);
        binding.setIdentifier(openid);
        binding.setCreateTime(LocalDateTime.now());
        userBindingDao.insert(binding);
        
        log.info("创建用户绑定关系成功: userId={}, openid={}", userId, openid);
        
        userRoleDao.insertUserRole(userId, MEMBER_ROLE_ID);
        
        log.info("初始化用户角色成功: userId={}, roleId={}", userId, MEMBER_ROLE_ID);
        
        return userId;
    }

    @Override
    public void saveLoginToken(String ticket, String token) {
        stringRedisTemplate.opsForValue().set(ticket, token, 5, TimeUnit.MINUTES);
    }

    @Override
    public String getLoginToken(String ticket) {
        String token = stringRedisTemplate.opsForValue().get(ticket);
        if (token != null) {
            stringRedisTemplate.delete(ticket);
        }
        return token;
    }
}
