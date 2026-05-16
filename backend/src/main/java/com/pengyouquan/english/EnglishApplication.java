package com.pengyouquan.english;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 英语剧场 - 启动类
 */
@SpringBootApplication
@EnableCaching
@EnableScheduling
public class EnglishApplication {

    public static void main(String[] args) {
        SpringApplication.run(EnglishApplication.class, args);
    }
}
