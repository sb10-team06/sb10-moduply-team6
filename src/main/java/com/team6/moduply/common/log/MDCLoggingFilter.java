package com.team6.moduply.common.log;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/// MDC 필터가 JWT 필터보다 먼저 실행돼야 JWT 인증 실패 로그에도 requestId가 찍힘.
/// HTTP 요청후 제일먼저 거치는 Filter
@Slf4j
@Component
/// 가장먼저 실행되는 필터
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MDCLoggingFilter extends OncePerRequestFilter {

    // MDC 로깅에 사용되는 상수 정의
    public static final String REQUEST_ID = "requestId";
    public static final String REQUEST_METHOD = "requestMethod";
    public static final String REQUEST_URI = "requestUri";
    public static final String REQUEST_ID_HEADER = "Moduply-Request-ID";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // 요청(request) ID 생성 (UUID)
        String requestId = UUID.randomUUID().toString().replace("-", "");
        // MDC에 컨텍스트 정보 추가
        MDC.put(REQUEST_ID, requestId);
        // HTTP Method 저장.
        MDC.put(REQUEST_METHOD, request.getMethod());
        // HTTP 요청 URI 저장.
        MDC.put(REQUEST_URI, request.getRequestURI());
        // 응답 헤더에 요청 ID 추가
        response.setHeader(REQUEST_ID_HEADER, requestId);

        try {
            log.info("요청 시작");
            filterChain.doFilter(request, response);
        } finally {
            /// 응답 반환후 MDC.clear
            log.info("요청 처리 완료");
            MDC.clear();
        }
    }
}
