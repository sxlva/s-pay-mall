package cn.fcr.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 配置类
 *
 * <p>手动配置 Lettuce 连接工厂及 StringRedisTemplate，
 * 覆盖 Spring Boot 默认自动配置。</p>
 *
 * @author 傅崇睿
 */
@Configuration
public class RedisConfig {

    /** Redis 主机地址 */
    @Value("${redis.sdk.config.host}")
    private String host;
    /** Redis 端口 */
    @Value("${redis.sdk.config.port}")
    private int port;
    /** Redis 密码 */
    @Value("${redis.sdk.config.password}")
    private String password;
    /** Redis 数据库索引 */
    @Value("${redis.sdk.config.database}")
    private int database;

    /**
     * 创建 Lettuce Redis 连接工厂
     *
     * @return RedisConnectionFactory
     */
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(host);
        config.setPort(port);
        config.setPassword(RedisPassword.of(password));
        config.setDatabase(database);

        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .build();

        return new LettuceConnectionFactory(config, clientConfig);
    }

    /**
     * 创建 StringRedisTemplate
     *
     * @param connectionFactory Redis 连接工厂
     * @return StringRedisTemplate，使用字符串序列化
     */
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        StringRedisTemplate template = new StringRedisTemplate();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }
}
