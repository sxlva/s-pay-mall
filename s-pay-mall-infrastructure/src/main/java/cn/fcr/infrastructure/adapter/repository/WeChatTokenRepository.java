package cn.fcr.infrastructure.adapter.repository;

import cn.fcr.domain.auth.repository.IWeChatTokenRepository;
import cn.fcr.types.common.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

@Slf4j
@Repository
public class WeChatTokenRepository implements IWeChatTokenRepository {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void saveBindTicket(String uuid, String openId) {
        String key = Constants.REDIS_WECHAT_BIND_TICKET_PREFIX + uuid;
        stringRedisTemplate.opsForValue().set(key, openId, 5, TimeUnit.MINUTES);
        log.info("保存微信绑定凭证 uuid:{} openId:{}", uuid, openId);
    }

    @Override
    public String getOpenIdByTicket(String uuid) {
        String key = Constants.REDIS_WECHAT_BIND_TICKET_PREFIX + uuid;
        String value = stringRedisTemplate.opsForValue().get(key);
        if (value != null && !Constants.REDIS_BIND_STATUS_PENDING.equals(value)) {
            return value;
        }
        return null;
    }

    @Override
    public void initBindStatus(String uuid) {
        String key = Constants.REDIS_WECHAT_BIND_TICKET_PREFIX + uuid;
        stringRedisTemplate.opsForValue().set(key, Constants.REDIS_BIND_STATUS_PENDING, 5, TimeUnit.MINUTES);
        log.info("初始化微信绑定状态 uuid:{}", uuid);
    }

    @Override
    public String getBindStatusRaw(String uuid) {
        String key = Constants.REDIS_WECHAT_BIND_TICKET_PREFIX + uuid;
        return stringRedisTemplate.opsForValue().get(key);
    }

    @Override
    public boolean tryAcquireRegisterLock(String username) {
        String key = Constants.REDIS_USER_REGISTER_LOCK_PREFIX + username;
        Boolean result = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 5, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(result);
    }

    @Override
    public void releaseRegisterLock(String username) {
        String key = Constants.REDIS_USER_REGISTER_LOCK_PREFIX + username;
        stringRedisTemplate.delete(key);
    }
}