package com.team6.moduply.user.controller;

import com.team6.moduply.common.error.ErrorResponse;
import com.team6.moduply.user.dto.UserCreateRequest;
import com.team6.moduply.user.dto.UserDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@Tag(name = "사용자 관리", description = "사용자 관련 API")
public interface UserApi {

  @Operation(
      summary = "사용자 등록 (회원가입)",
      description = "신규 사용자를 등록합니다.",
      operationId = "createUser"
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "201",
          description = "성공",
          content = @Content(
              mediaType = MediaType.ALL_VALUE,
              schema = @Schema(implementation = UserDto.class)
          )
      ),
      @ApiResponse(
          responseCode = "200",
          description = "성공",
          content = @Content(
              mediaType = MediaType.ALL_VALUE,
              schema = @Schema(implementation = UserDto.class)
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
          responseCode = "500",
          description = "서버 오류",
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
      )
  })
  ResponseEntity<UserDto> postUser(UserCreateRequest request);
}
