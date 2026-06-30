package com.team6.moduply.common.config;

import com.team6.moduply.auth.filter.JwtAuthenticationFilter;
import com.team6.moduply.auth.handler.JwtLoginSuccessHandler;
import com.team6.moduply.auth.handler.JwtLogoutHandler;
import com.team6.moduply.auth.handler.ModuPlyAccessDeniedHandler;
import com.team6.moduply.auth.handler.ModuPlyAuthenticationEntryPoint;
import com.team6.moduply.auth.handler.ModuPlyLoginFailureHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
  private final JwtAuthenticationFilter jwtAuthenticationFilter;
  private final ModuPlyAuthenticationEntryPoint moduPlyAuthenticationEntryPoint;
  private final ModuPlyAccessDeniedHandler moduPlyAccessDeniedHandler;
  private final ModuPlyLoginFailureHandler moduPlyLoginFailureHandler;
  private final JwtLogoutHandler jwtLogoutHandler;
  private final JwtLoginSuccessHandler jwtLoginSuccessHandler;
  private final CsrfTokenRepository csrfTokenRepository;
  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.
        csrf(csrf -> csrf
            .csrfTokenRepository(csrfTokenRepository)
            .ignoringRequestMatchers(
                new AntPathRequestMatcher("/api/users", "POST"),
                new AntPathRequestMatcher("/api/auth/sign-in", "POST"),
                // TODO: 비밀번호 재발급 API CSRF 검증 정책은 추후 논의 후 변경
                new AntPathRequestMatcher("/api/auth/reset-password", "POST"),
                // TODO: refresh token 재발급 API 구현 후 CSRF 헤더 검증 흐름으로 변경
                new AntPathRequestMatcher("/api/auth/refresh", "POST"),
                new AntPathRequestMatcher("/api/auth/sign-out", "POST"),
                new AntPathRequestMatcher("/ws/**"),
                new AntPathRequestMatcher("/api/contents", "POST"),
                new AntPathRequestMatcher("/api/contents/*", "PATCH")
            )
        )
        .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .formLogin(login -> login
            .loginProcessingUrl("/api/auth/sign-in")
            .usernameParameter("username")
            .passwordParameter("password")
            .successHandler(jwtLoginSuccessHandler)
            .failureHandler(moduPlyLoginFailureHandler))
        .logout(logout -> logout
            .logoutUrl("/api/auth/sign-out")
            .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT))
            .addLogoutHandler(jwtLogoutHandler))
        .authorizeHttpRequests(auth ->
            // 인증,인가 관련된 api 인증 불필요
            auth.requestMatchers("/api/auth/**").permitAll()
                // 회원가입 인증 불필요
                .requestMatchers(HttpMethod.POST, "/api/users").permitAll()
                // 문서 관련 api 인증 불필요
                .requestMatchers("/swagger-ui/**").permitAll()
                // 정적 리소스 접근 인증 불필요
                .requestMatchers("/", "/index.html", "/favicon.svg", "/assets/**").permitAll()
                // 로컬 저장소 업로드 파일 접근 인증 불필요
                .requestMatchers(HttpMethod.GET, "/uploads/**").permitAll()
                .requestMatchers(HttpMethod.HEAD, "/uploads/**").permitAll()
                // sse 관련 api 인증 불필요
                .requestMatchers("/api/sse/**").permitAll()
                // TODO: WebSocket STOMP CONNECT JWT 검증 구현 후 인가 정책 재검토
                .requestMatchers("/ws/**").permitAll()
                // 그외는 모두 인증 요구
                .anyRequest().authenticated())
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
        .exceptionHandling(exception ->
            exception.accessDeniedHandler(moduPlyAccessDeniedHandler)
                    .authenticationEntryPoint(moduPlyAuthenticationEntryPoint));

    return http.build();
  }
}
