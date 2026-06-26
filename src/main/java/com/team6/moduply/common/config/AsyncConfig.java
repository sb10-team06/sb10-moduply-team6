package com.team6.moduply.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
public class AsyncConfig {
    //TODO: 비동기작업 스레드풀 설정 필요.

    //TODO: 비동기 스레드에서도 requestId와 로그인 사용자 정보를 유지 설정 필요

    //TODO: 스레드 풀 상태를 Actuator/Micrometer로 모니터링하는 설정 필요
}
