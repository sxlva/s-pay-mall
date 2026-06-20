package cn.fcr.infrastructure.auth.gateway;

import cn.fcr.domain.auth.gateway.IWechatLoginGateway;
import cn.fcr.infrastructure.dao.IMallUserDao;
import cn.fcr.infrastructure.dao.IUserBindingDao;
import cn.fcr.infrastructure.dao.IUserRoleDao;
import cn.fcr.infrastructure.dao.po.MallUser;
import cn.fcr.infrastructure.dao.po.UserBinding;
import cn.fcr.types.common.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @description 微信登录网关实现类
 *
 * 【职责说明】
 * - 处理微信扫码登录相关的持久化和缓存操作
 * - 实现领域层定义的IWechatLoginGateway接口
 * - 负责用户绑定关系管理、新用户创建、登录token缓存等功能
 *
 * 【核心功能】
 * 1. findUserIdByOpenid(): 根据OpenID查找用户ID
 * 2. createWechatUserAndBind(): 创建微信用户并建立绑定关系
 * 3. saveLoginToken(): 保存登录token到Redis
 * 4. getLoginToken(): 获取并删除登录token
 *
 * 【依赖说明】
 * - IMallUserDao: 用户数据访问接口
 * - IUserBindingDao: 用户绑定关系数据访问接口
 * - IUserRoleDao: 用户角色数据访问接口
 * - StringRedisTemplate: Redis操作模板
 */
@Slf4j
@Component
public class WeixinLoginGatewayImpl implements IWechatLoginGateway {

    @Resource
    private IMallUserDao mallUserDao;

    @Resource
    private IUserBindingDao userBindingDao;

    @Resource
    private IUserRoleDao userRoleDao;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 会员角色ID
     */
    private static final Long MEMBER_ROLE_ID = 2L;

    @Override
    public Long findUserIdByOpenid(String openid) {
        UserBinding binding = userBindingDao.findByIdentityTypeAndIdentifier(
                Constants.IDENTITY_TYPE_WECHAT_MP, openid);
        return binding != null ? binding.getUserId() : null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createWechatUserAndBind(String openid) {
        log.info("创建微信用户并绑定: openid={}", openid);

        // 1. 创建临时用户记录
        MallUser newUser = new MallUser();
        newUser.setUsername("temp_" + UUID.randomUUID().toString().substring(0, 8));
        newUser.setPassword("");
        newUser.setStatus(Constants.USER_STATUS_WECHAT);
        newUser.setCreateTime(LocalDateTime.now());
        newUser.setUpdateTime(LocalDateTime.now());
        mallUserDao.insert(newUser);

        // 2. 获取自增ID并更新用户名
        Long userId = newUser.getId();
        String defaultUsername = "wx_user_" + userId;
        newUser.setUsername(defaultUsername);
        mallUserDao.updateById(newUser);

        log.info("创建新用户成功: userId={}, username={}", userId, defaultUsername);

        // 3. 创建用户绑定关系
        UserBinding binding = new UserBinding();
        binding.setUserId(userId);
        binding.setIdentityType(Constants.IDENTITY_TYPE_WECHAT_MP);
        binding.setIdentifier(openid);
        binding.setCreateTime(LocalDateTime.now());
        userBindingDao.insert(binding);

        log.info("创建用户绑定关系成功: userId={}, openid={}", userId, openid);

        // 4. 初始化用户角色
        userRoleDao.insertUserRole(userId, MEMBER_ROLE_ID);

        log.info("初始化用户角色成功: userId={}, roleId={}", userId, MEMBER_ROLE_ID);

        return userId;
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
