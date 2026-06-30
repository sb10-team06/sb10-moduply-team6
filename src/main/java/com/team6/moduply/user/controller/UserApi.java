package com.team6.moduply.user.controller;

import com.team6.moduply.common.error.ErrorResponse;
import com.team6.moduply.user.dto.ChangePasswordRequest;
import com.team6.moduply.user.dto.UserCreateRequest;
import com.team6.moduply.user.dto.UserDto;
import com.team6.moduply.user.dto.UserLockUpdateRequest;
import com.team6.moduply.user.dto.UserRoleUpdateRequest;
import com.team6.moduply.user.dto.UserUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

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

  @Operation(
      summary = "사용자 단건 조회",
      description = "사용자 ID로 사용자를 조회합니다.",
      operationId = "getUser"
  )
  @ApiResponses({
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
          responseCode = "404",
          description = "사용자를 찾을 수 없음",
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
  ResponseEntity<UserDto> getUser(
      @Parameter(description = "사용자 ID", required = true)
      UUID userId
  );

  @Operation(
      summary = "[어드민] 권한 수정",
      operationId = "updateUser_Role"
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
          responseCode = "401",
          description = "인증 오류",
          content = @Content(
              mediaType = MediaType.ALL_VALUE,
              schema = @Schema(implementation = ErrorResponse.class)
          )
      ),
      @ApiResponse(
          responseCode = "403",
          description = "권한 오류",
          content = @Content(
              mediaType = MediaType.ALL_VALUE,
              schema = @Schema(implementation = ErrorResponse.class)
          )
      ),
      @ApiResponse(
          responseCode = "404",
          description = "해당 리소스 없음",
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
  ResponseEntity<Void> updateUserRole(
      @Parameter(description = "사용자 ID", required = true)
      UUID userId,
      @RequestBody(
          required = true,
          content = @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = UserRoleUpdateRequest.class)
          )
      )
      UserRoleUpdateRequest request
  );

  @Operation(
      summary = "[어드민] 계정 잠금 상태 변경",
      description = "[어드민 기능] 계정 잠금 상태를 변경합니다.",
      operationId = "updateUser_Locked"
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
          responseCode = "401",
          description = "인증 오류",
          content = @Content(
              mediaType = MediaType.ALL_VALUE,
              schema = @Schema(implementation = ErrorResponse.class)
          )
      ),
      @ApiResponse(
          responseCode = "403",
          description = "권한 오류",
          content = @Content(
              mediaType = MediaType.ALL_VALUE,
              schema = @Schema(implementation = ErrorResponse.class)
          )
      ),
      @ApiResponse(
          responseCode = "404",
          description = "해당 리소스 없음",
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
  ResponseEntity<Void> updateUserLocked(
      @Parameter(description = "사용자 ID", required = true)
      UUID userId,
      @RequestBody(
          required = true,
          content = @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = UserLockUpdateRequest.class)
          )
      )
      UserLockUpdateRequest request
  );

  @Operation(
      summary = "프로필 변경",
      description = "본인의 프로필만 변경할 수 있습니다.",
      operationId = "updateUser"
  )
  @ApiResponses({
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
          responseCode = "401",
          description = "인증 오류",
          content = @Content(
              mediaType = MediaType.ALL_VALUE,
              schema = @Schema(implementation = ErrorResponse.class)
          )
      ),
      @ApiResponse(
          responseCode = "403",
          description = "권한 오류",
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
  ResponseEntity<UserDto> updateUser(
      @Parameter(description = "사용자 ID", required = true)
      @PathVariable UUID userId,
      @Parameter(description = "프로필 변경 요청", required = true)
      @RequestPart("request") UserUpdateRequest request,
      @Parameter(description = "프로필 이미지")
      @RequestPart(value = "image", required = false)
      MultipartFile image
  );

  @Operation(
      summary = "비밀번호 변경",
      description = "본인의 비밀번호만 변경할 수 있습니다.",
      operationId = "updateUser_Password"
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
          responseCode = "401",
          description = "인증 오류",
          content = @Content(
              mediaType = MediaType.ALL_VALUE,
              schema = @Schema(implementation = ErrorResponse.class)
          )
      ),
      @ApiResponse(
          responseCode = "403",
          description = "권한 오류",
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
  ResponseEntity<Void> updateUserPassword(
      @Parameter(description = "사용자 ID", required = true)
      @PathVariable UUID userId,
      @RequestBody(
          required = true,
          content = @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = ChangePasswordRequest.class)
          )
      )
      ChangePasswordRequest request);

}
