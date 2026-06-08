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
 */
@Configuration
public class RedisConfig {

    // 1. 读取自定义配置
    @Value("${redis.sdk.config.host}")
    private String host;
    @Value("${redis.sdk.config.port}")
    private int port;
    @Value("${redis.sdk.config.password}")
    private String password;
    @Value("${redis.sdk.config.database}")
    private int database;

    // 2. 创建并配置连接工厂 (关键点：这里必须手动设置连接参数)
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(host);
        config.setPort(port);
        config.setPassword(RedisPassword.of(password));
        config.setDatabase(database);

        // 如果你需要配置连接池 (Lettuce 需要添加 commons-pool2 依赖)
        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .build();

        return new LettuceConnectionFactory(config, clientConfig);
    }

    // 3. 使用配置好的连接工厂
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