package com.pengyouquan.english.config;

import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;

/**
 * 应用信息贡献者
 * 通过 Actuator /info 端点暴露应用信息
 */
@Component
public class AppInfoConfig implements InfoContributor {

    @Override
    public void contribute(Info.Builder builder) {
        builder.withDetail("app", "朋友圈英语")
               .withDetail("version", "1.0.0")
               .withDetail("java", System.getProperty("java.version"));
    }
}
