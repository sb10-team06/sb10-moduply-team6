package com.team6.moduply.common.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

/// 요청(Request) 단위로 로그 추적 정보를 MDC에 저장해서 모든 로그에 같은 식별자를 남기기 위한 인터셉터
/// 사용자 요청 하나에 고유ID를 부여하고, 그 요청 처리중 발생하는 모든 로그를 하나의 흐름으로 묶기위한 기능.
/// Spring MVC 요청 처리 흐름에 끼어들기 위한 인터셉터.
/// HTTP 요청 -> Filter -> Interceptor preHandle() -> Controller -> Service -> Repository -> Interceptor afterCompletion() -> 응답.
@Slf4j
public class MDCLoggingInterceptor implements HandlerInterceptor {

    //MDC 로깅에 사용되는 상수 정의
    public static final String REQUEST_ID = "requestId";
    public static final String REQUEST_METHOD = "requestMethod";
    public static final String REQUEST_URI = "requestUri";
    public static final String REQUEST_ID_HEADER = "Discodeit-Request-ID";

    /// 요청이 Controller 도착하기전에 실행
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 요청(request) ID 생성 (UUID)
        String requestId = UUID.randomUUID().toString().replaceAll("-", "");

        // MDC에 컨텍스트 정보 추가
        MDC.put(REQUEST_ID, requestId);
        // HTTP Method 저장.
        MDC.put(REQUEST_METHOD, request.getMethod());
        // HTTP 요청 URI 저장.
        MDC.put(REQUEST_URI, request.getRequestURI());

        // 응답 헤더에 요청 ID 추가
        response.setHeader(REQUEST_ID_HEADER, requestId);

        log.debug("요청 시작");
        return true;
    }

    /// Controller 처리 끝나고 실행
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 요청 처리 후 MDC 데이터 정리
        log.debug("요청처리 완료.");
        MDC.clear();
    }


}
