package com.team6.moduply.watching.controller.api;

import com.team6.moduply.common.error.ErrorResponse;
import com.team6.moduply.common.pagination.CursorResponse;
import com.team6.moduply.watching.dto.WatchingSessionDto;
import com.team6.moduply.watching.dto.WatchingSessionQueryCondition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(name = "시청 세션 관리", description = "시청 세션 관련 API")
@Validated
public interface WatchingSessionApi {

  @Operation(
      summary = "특정 사용자의 시청 세션 조회",
      description = "특정 사용자의 시청 세션을 조회합니다.",
      security = {
          @SecurityRequirement(name = "jwtToken")
      }
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "성공"),
      @ApiResponse(responseCode = "400", description = "잘못된 요청",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "500", description = "서버 오류",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "401", description = "인증 오류",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  @Parameters({
      @Parameter(name = "watcherId")
  })
  @GetMapping("/users/{watcherId}/watching-sessions")
  ResponseEntity<WatchingSessionDto> findWatchingSessionsByWatcher(
      @PathVariable UUID watcherId);

  @Operation(
      summary = "특정 콘텐츠의 시청 세션 목록 조회 (커서 페이지네이션)",
      description = "특정 콘텐츠의 시청 세션 목록을 조회합니다.",
      security = {
          @SecurityRequirement(name = "jwtToken")
      }
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "성공"),
      @ApiResponse(responseCode = "400", description = "잘못된 요청",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "500", description = "서버 오류",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "401", description = "인증 오류",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  @Parameters({
      @Parameter(name = "contentId")
  })
  @GetMapping("/contents/{contentId}/watching-sessions")
  ResponseEntity<CursorResponse<WatchingSessionDto>> findWatchingSessionsByContent(
      @PathVariable UUID contentId,
      @ParameterObject
      @Valid
      @ModelAttribute WatchingSessionQueryCondition condition);
}
