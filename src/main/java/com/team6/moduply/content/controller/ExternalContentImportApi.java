package com.team6.moduply.content.controller;

import com.team6.moduply.common.error.ErrorResponse;
import com.team6.moduply.content.external.ExternalContentImportResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@Tag(name = "외부 콘텐츠 수집", description = "외부 Open API 콘텐츠 수집 API")
public interface ExternalContentImportApi {

  @Operation(
      summary = "[어드민] TMDB 영화 수동 수집",
      description = """
          TMDB 인기 영화 데이터를 수동으로 수집합니다.

          현재 수집 범위는 TMDB popular movie 목록 기준입니다.
          page 값으로 수집할 단일 목록 페이지를 선택하고,
          language 값으로 제목/설명 응답 언어를 선택합니다.

          예시: page=1, language=ko-KR
          """,
      operationId = "importTmdbMovies",
      security = {
          @SecurityRequirement(name = "csrfToken"),
          @SecurityRequirement(name = "jwtToken")
      }
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "성공",
          content = @Content(mediaType = MediaType.ALL_VALUE,
              schema = @Schema(implementation = ExternalContentImportResult.class))),
      @ApiResponse(responseCode = "400", description = "잘못된 요청",
          content = @Content(mediaType = MediaType.ALL_VALUE,
              schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "401", description = "인증 오류",
          content = @Content(mediaType = MediaType.ALL_VALUE,
              schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "403", description = "권한 오류",
          content = @Content(mediaType = MediaType.ALL_VALUE,
              schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "500", description = "서버 오류",
          content = @Content(mediaType = MediaType.ALL_VALUE,
              schema = @Schema(implementation = ErrorResponse.class)))
  })
  ResponseEntity<ExternalContentImportResult> importTmdbMovies(
      @Parameter(description = "TMDB 인기 영화 목록 조회 페이지", example = "1",
          schema = @Schema(defaultValue = "1")) Integer page,
      @Parameter(description = "TMDB 응답 언어. 예시: ko-KR, en-US, ja-JP", example = "ko-KR",
          schema = @Schema(defaultValue = "ko-KR")) String language
  );

  @Operation(
      summary = "[어드민] TMDB TV 수동 수집",
      description = """
          TMDB 인기 TV 데이터를 수동으로 수집합니다.

          현재 수집 범위는 TMDB popular TV 목록 기준입니다.
          page 값으로 수집할 단일 목록 페이지를 선택하고,
          language 값으로 제목/설명 응답 언어를 선택합니다.

          예시: page=1, language=ko-KR
          """,
      operationId = "importTmdbTvSeries",
      security = {
          @SecurityRequirement(name = "csrfToken"),
          @SecurityRequirement(name = "jwtToken")
      }
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "성공",
          content = @Content(mediaType = MediaType.ALL_VALUE,
              schema = @Schema(implementation = ExternalContentImportResult.class))),
      @ApiResponse(responseCode = "400", description = "잘못된 요청",
          content = @Content(mediaType = MediaType.ALL_VALUE,
              schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "401", description = "인증 오류",
          content = @Content(mediaType = MediaType.ALL_VALUE,
              schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "403", description = "권한 오류",
          content = @Content(mediaType = MediaType.ALL_VALUE,
              schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "500", description = "서버 오류",
          content = @Content(mediaType = MediaType.ALL_VALUE,
              schema = @Schema(implementation = ErrorResponse.class)))
  })
  ResponseEntity<ExternalContentImportResult> importTmdbTvSeries(
      @Parameter(description = "TMDB 인기 TV 목록 조회 페이지", example = "1",
          schema = @Schema(defaultValue = "1")) Integer page,
      @Parameter(description = "TMDB 응답 언어. 예시: ko-KR, en-US, ja-JP", example = "ko-KR",
          schema = @Schema(defaultValue = "ko-KR")) String language
  );

  @Operation(
      summary = "[어드민] The Sports DB 리그 경기 수동 수집",
      description = """
          The Sports DB 리그 다음 경기 데이터를 수동으로 수집합니다.

          leagueId 값으로 특정 리그의 경기만 수집합니다.
          leagueId는 필수값이며 공백은 허용하지 않습니다.
          예시: 4328(EPL), 4387(NBA), 4424(MLB)
          """,
      operationId = "importSportsDbLeagueEvents",
      security = {
          @SecurityRequirement(name = "csrfToken"),
          @SecurityRequirement(name = "jwtToken")
      }
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "성공",
          content = @Content(mediaType = MediaType.ALL_VALUE,
              schema = @Schema(implementation = ExternalContentImportResult.class))),
      @ApiResponse(responseCode = "400", description = "잘못된 요청",
          content = @Content(mediaType = MediaType.ALL_VALUE,
              schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "401", description = "인증 오류",
          content = @Content(mediaType = MediaType.ALL_VALUE,
              schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "403", description = "권한 오류",
          content = @Content(mediaType = MediaType.ALL_VALUE,
              schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "500", description = "서버 오류",
          content = @Content(mediaType = MediaType.ALL_VALUE,
              schema = @Schema(implementation = ErrorResponse.class)))
  })
  ResponseEntity<ExternalContentImportResult> importSportsDbLeagueEvents(
      @Parameter(description = "The Sports DB 리그 ID. 공백 불가. 예시: 4328(EPL), 4387(NBA), 4424(MLB)",
          example = "4328") String leagueId
  );

  @Operation(
      summary = "[어드민] The Sports DB 일별 경기 수동 수집",
      description = """
          The Sports DB 일별 경기 데이터를 수동으로 수집합니다.

          date 값으로 수집 날짜를 지정합니다.
          sport 값을 입력하면 특정 종목만 수집하고, leagueId 값을 입력하면 특정 리그만 수집합니다.
          sport, leagueId는 선택값이므로 날짜 기준 전체 경기 수집도 가능합니다.
          선택값에 공백만 전달되면 입력하지 않은 값으로 처리합니다.

          예시: date=2026-07-02, sport=Soccer, leagueId=4328
          """,
      operationId = "importSportsDbDayEvents",
      security = {
          @SecurityRequirement(name = "csrfToken"),
          @SecurityRequirement(name = "jwtToken")
      }
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "성공",
          content = @Content(mediaType = MediaType.ALL_VALUE,
              schema = @Schema(implementation = ExternalContentImportResult.class))),
      @ApiResponse(responseCode = "400", description = "잘못된 요청",
          content = @Content(mediaType = MediaType.ALL_VALUE,
              schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "401", description = "인증 오류",
          content = @Content(mediaType = MediaType.ALL_VALUE,
              schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "403", description = "권한 오류",
          content = @Content(mediaType = MediaType.ALL_VALUE,
              schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "500", description = "서버 오류",
          content = @Content(mediaType = MediaType.ALL_VALUE,
              schema = @Schema(implementation = ErrorResponse.class)))
  })
  ResponseEntity<ExternalContentImportResult> importSportsDbDayEvents(
      @Parameter(description = "수집할 경기 날짜", example = "2026-07-02")
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
      @Parameter(description = "The Sports DB 종목 필터. 예시: Soccer, Basketball, Baseball",
          example = "Soccer") String sport,
      @Parameter(description = "The Sports DB 리그 ID 필터. 예시: 4328(EPL), 4387(NBA), 4424(MLB)",
          example = "4328") String leagueId
  );
}
