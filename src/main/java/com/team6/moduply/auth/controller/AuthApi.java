package com.team6.moduply.auth.controller;

import com.team6.moduply.auth.dto.JwtDto;
import com.team6.moduply.auth.dto.ResetPasswordRequest;
import com.team6.moduply.common.error.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;

@Tag(name = "인증 관리", description = "인증 관련 API")
public interface AuthApi {

  @Operation(
      summary = "CSRF 토큰 조회",
      description = "CSRF 토큰을 조회합니다. 토큰은 쿠키(XSRF-TOKEN)에 저장됩니다.",
      operationId = "getCsrfToken"
  )
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "성공"),
      @ApiResponse(responseCode = "200", description = "성공"),
      @ApiResponse(
          responseCode = "400",
          description = "잘못된 요청",
          content = @Content(
              mediaType = MediaType.ALL_VALUE,
              schema = @Schema(implementation = ErrorResponse.class)
          )
      ),
      @ApiResponse(
          responseCode = "500",
          description = "서버 오류",
          content = @Content(
              mediaType = MediaType.ALL_VALUE,
              schema = @Schema(implementation = ErrorResponse.class)
          )
      )
  })
  ResponseEntity<Void> getCsrfToken(
      @Parameter(hidden = true)
      CsrfToken csrfToken
  );

  @Operation(
      summary = "토큰 재발급",
      description = "쿠키(REFRESH_TOKEN)에 저장된 리프레시 토큰으로 엑세스 토큰을 재발급합니다.",
      operationId = "reissueToken"
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "성공",
          content = @Content(
              mediaType = MediaType.ALL_VALUE,
              schema = @Schema(implementation = JwtDto.class)
          )
      ),
      @ApiResponse(
          responseCode = "400",
          description = "잘못된 요청",
          content = @Content(
              mediaType = MediaType.ALL_VALUE,
              schema = @Schema(implementation = ErrorResponse.class)
          )
      ),
      @ApiResponse(
          responseCode = "401",
          description = "인증 오류",
          content = @Content(
              mediaType = MediaType.ALL_VALUE,
              schema = @Schema(implementation = ErrorResponse.class)
          )
      ),
      @ApiResponse(
          responseCode = "500",
          description = "서버 오류",
          content = @Content(
              mediaType = MediaType.ALL_VALUE,
              schema = @Schema(implementation = ErrorResponse.class)
          )
      )
  })
  ResponseEntity<JwtDto> refreshAccessToken(
      @Parameter(
          name = "REFRESH_TOKEN",
          description = "리프레시 토큰",
          required = true,
          in = ParameterIn.COOKIE
      )
      String refreshToken,
      HttpServletResponse response
  );

  @Operation(
      summary = "비밀번호 초기화",
      description = "임시 비밀번호를 발급하고 이메일로 전송합니다.",
      operationId = "resetPassword"
  )
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "성공"),
      @ApiResponse(
          responseCode = "400",
          description = "잘못된 요청",
          content = @Content(
              mediaType = MediaType.ALL_VALUE,
              schema = @Schema(implementation = ErrorResponse.class)
          )
      ),
      @ApiResponse(
          responseCode = "500",
          description = "서버 오류",
          content = @Content(
              mediaType = MediaType.ALL_VALUE,
              schema = @Schema(implementation = ErrorResponse.class)
          )
      )
  })
  ResponseEntity<Void> resetPassword(
      @RequestBody(
          required = true,
          content = @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = ResetPasswordRequest.class)
          )
      )
      ResetPasswordRequest request
  );
}
