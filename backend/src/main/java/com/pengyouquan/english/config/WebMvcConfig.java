package com.pengyouquan.english.config;

import com.pengyouquan.english.security.CurrentUserIdResolver;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Web MVC 配置
 * 注册自定义参数解析器、拦截器等
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final CurrentUserIdResolver currentUserIdResolver;
    private final RequestLoggingInterceptor requestLoggingInterceptor;

    public WebMvcConfig(CurrentUserIdResolver currentUserIdResolver,
                        RequestLoggingInterceptor requestLoggingInterceptor) {
        this.currentUserIdResolver = currentUserIdResolver;
        this.requestLoggingInterceptor = requestLoggingInterceptor;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentUserIdResolver);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册请求日志拦截器，统计所有 API 请求（排除静态资源）
        registry.addInterceptor(requestLoggingInterceptor)
                .addPathPatterns("/api/**");
    }
}
