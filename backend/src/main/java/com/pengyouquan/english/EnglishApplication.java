package com.pengyouquan.english;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

/**
 * 英语剧场 - 启动类
 */
@SpringBootApplication
@EnableCaching
public class EnglishApplication {

    public static void main(String[] args) {
        SpringApplication.run(EnglishApplication.class, args);
    }
}
