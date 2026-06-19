package com.team6.moduply.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


/// 웹 MVC 설정 클래스
/// 모든 HTTP 요청이 Controller에 들어가기전에 MDCLoggingInterceptor를 거치도록 Spring에 등록.
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /// MDCLoggingInterceptor을 Bean으로 등록
    @Bean
    public MDCLoggingInterceptor mdcLoggingInterceptor() {
        return new MDCLoggingInterceptor();
    }

    /// 모든 요청 Controller 전에 mdcLoggingInterceptor로 설정.
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(mdcLoggingInterceptor())
                .addPathPatterns("/**"); // 모든 경로에 적용
    }
}
