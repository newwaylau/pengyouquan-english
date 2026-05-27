package com.pengyouquan.english.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 测试环境 Redis mock 配置。
 * RedisHealthIndicator 需要 RedisConnectionFactory bean，
 * 测试时用 mock 替代避免连接真实 Redis。
 */
@Configuration
@Profile("test")
public class TestRedisConfig {

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisConnectionFactory factory = mock(RedisConnectionFactory.class);
        RedisConnection connection = mock(RedisConnection.class);
        when(factory.getConnection()).thenReturn(connection);
        when(connection.ping()).thenReturn("PONG");
        return factory;
    }
}
