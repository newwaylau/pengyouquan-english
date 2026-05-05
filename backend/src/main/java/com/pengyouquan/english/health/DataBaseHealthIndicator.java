package com.pengyouquan.english.health;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * 数据库健康指示器
 * 检查数据库连接是否可用
 */
@Component
public class DataBaseHealthIndicator implements HealthIndicator {

    @Autowired
    private DataSource dataSource;

    @Override
    public Health health() {
        try (Connection conn = dataSource.getConnection()) {
            if (conn.isValid(3)) {
                return Health.up()
                        .withDetail("database", conn.getMetaData().getDatabaseProductName())
                        .withDetail("url", conn.getMetaData().getURL())
                        .build();
            } else {
                return Health.down()
                        .withDetail("reason", "数据库连接验证失败")
                        .build();
            }
        } catch (Exception e) {
            return Health.down(e)
                    .withDetail("reason", "无法连接到数据库")
                    .build();
        }
    }
}
