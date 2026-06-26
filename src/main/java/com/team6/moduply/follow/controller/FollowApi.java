package com.team6.moduply.follow.controller;

import com.team6.moduply.common.error.ErrorResponse;
import com.team6.moduply.follow.dto.FollowDto;
import com.team6.moduply.follow.dto.FollowRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@Tag(name = "팔로우 관리", description = "사용자 팔로우 관계 관리 API")
public interface FollowApi {

  @Operation(
      summary = "팔로우",
      description = "취향이 비슷한 다른 사용자를 팔로우합니다.",
      operationId = "createFollow"
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "201",
          description = "성공",
          content = @Content(
              mediaType = MediaType.ALL_VALUE,
              schema = @Schema(implementation = FollowDto.class)
          )
      ),
      @ApiResponse(
          responseCode = "200",
          description = "성공",
          content = @Content(
              mediaType = MediaType.ALL_VALUE,
              schema = @Schema(implementation = FollowDto.class)
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
          responseCode = "409",
          description = "이미 존재하는 팔로우 관계",
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

  // TODO: JWT 인증 연동 후 요청 헤더가 아니라 인증 객체에서 followerId를 가져오도록 변경한다.
  // TODO: @AuthenticationPrincipal 사용 시 Swagger 문서에는 노출되지 않도록 @Parameter(hidden = true)를 적용한다.
  ResponseEntity<FollowDto> createFollow(
      FollowRequest request,
      @Parameter(description = "팔로우 요청자 ID", required = true)
      UUID followerId
  );
}
