package com.pengyouquan.english.health;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;

/**
 * Redis 健康指示器
 * 检查 Redis 连接是否可用
 */
@Component
public class RedisHealthIndicator implements HealthIndicator {

    @Autowired
    private RedisConnectionFactory redisConnectionFactory;

    @Override
    public Health health() {
        try (RedisConnection conn = redisConnectionFactory.getConnection()) {
            String pong = conn.ping();
            if ("PONG".equalsIgnoreCase(pong)) {
                return Health.up()
                        .withDetail("redis", "连接正常")
                        .withDetail("ping", pong)
                        .build();
            } else {
                return Health.down()
                        .withDetail("reason", "Redis ping 返回异常: " + pong)
                        .build();
            }
        } catch (Exception e) {
            return Health.down(e)
                    .withDetail("reason", "无法连接到 Redis")
                    .build();
        }
    }
}
