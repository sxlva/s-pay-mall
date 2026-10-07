package cn.fcr.infrastructure.auth.login.repository;

import cn.fcr.domain.auth.login.repository.IWeChatTokenRepository;
import cn.fcr.types.common.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * 微信Token仓储实现（Redis缓存）
 *
 * @author 傅崇睿
 */
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
    public void clearBindStatus(String uuid) {
        String key = Constants.REDIS_WECHAT_BIND_TICKET_PREFIX + uuid;
        stringRedisTemplate.delete(key);
        log.info("清除微信绑定票据 uuid:{}", uuid);
    }
}
