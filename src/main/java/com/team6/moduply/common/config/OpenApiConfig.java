package com.team6.moduply.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI openAPI() {

    Server prodServer = new Server()
        .url("https://moduply.co.kr") // 실제 배포 도메인 주소
        .description("배포 서버 (HTTPS)");

    Server localServer = new Server()
        .url("http://localhost:8080")
        .description("로컬 서버");

    //csrf
    SecurityScheme csrfScheme = new SecurityScheme()
        .type(SecurityScheme.Type.APIKEY)
        .in(SecurityScheme.In.HEADER)
        .name("X-XSRF-TOKEN")
        .description("CSRF 토큰 (GET /api/auth/csrf-token 호출 후 XSRF-TOKEN 쿠키 값)");

    //jwt
    SecurityScheme jwtScheme = new SecurityScheme()
        .type(SecurityScheme.Type.HTTP)
        .scheme("bearer")
        .bearerFormat("JWT")
        .in(SecurityScheme.In.HEADER)
        .name("Authorization")
        .description("JWT 액세스 토큰 (로그인 후 발급)");

    return new OpenAPI()
        .info(new Info()
            .title("모두의 플레이리스트 API 문서")
            .description("모두의 플레이리스트 프로젝트의 Swagger API 문서입니다.")
            .version("1.0")
        )
        .servers(List.of(prodServer, localServer))
        .components(new Components()
            .addSecuritySchemes("csrfToken", csrfScheme)
            .addSecuritySchemes("jwtToken", jwtScheme)
        );
  }
}
