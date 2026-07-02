package com.team6.moduply.content.controller;

import com.team6.moduply.content.external.ExternalContentImportResult;
import com.team6.moduply.content.external.ExternalContentManualImportService;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequestMapping("/api/contents/import")
@RequiredArgsConstructor
public class ExternalContentImportController implements ExternalContentImportApi {

  private final ExternalContentManualImportService externalContentManualImportService;

  @PostMapping(value = "/tmdb/movies", produces = MediaType.APPLICATION_JSON_VALUE)
  @Override
  public ResponseEntity<ExternalContentImportResult> importTmdbMovies(
      @RequestParam(defaultValue = "1") Integer page,
      @RequestParam(defaultValue = "ko-KR") String language
  ) {
    log.info("TMDB 영화 수동 수집 요청 수신: page={}, language={}", page, language);
    ExternalContentImportResult response = externalContentManualImportService.importTmdbMovies(
        page,
        language
    );
    log.info("TMDB 영화 수동 수집 요청 처리 완료: savedCount={}", response.savedCount());

    return ResponseEntity.ok(response);
  }

  @PostMapping(value = "/tmdb/tv-series", produces = MediaType.APPLICATION_JSON_VALUE)
  @Override
  public ResponseEntity<ExternalContentImportResult> importTmdbTvSeries(
      @RequestParam(defaultValue = "1") Integer page,
      @RequestParam(defaultValue = "ko-KR") String language
  ) {
    log.info("TMDB TV 수동 수집 요청 수신: page={}, language={}", page, language);
    ExternalContentImportResult response = externalContentManualImportService.importTmdbTvSeries(
        page,
        language
    );
    log.info("TMDB TV 수동 수집 요청 처리 완료: savedCount={}", response.savedCount());

    return ResponseEntity.ok(response);
  }

  @PostMapping(value = "/sports-db/league-events", produces = MediaType.APPLICATION_JSON_VALUE)
  @Override
  public ResponseEntity<ExternalContentImportResult> importSportsDbLeagueEvents(
      @RequestParam String leagueId
  ) {
    log.info("The Sports DB 리그 경기 수동 수집 요청 수신: leagueId={}", leagueId);
    ExternalContentImportResult response =
        externalContentManualImportService.importSportsDbLeagueEvents(leagueId);
    log.info("The Sports DB 리그 경기 수동 수집 요청 처리 완료: savedCount={}", response.savedCount());

    return ResponseEntity.ok(response);
  }

  @PostMapping(value = "/sports-db/day-events", produces = MediaType.APPLICATION_JSON_VALUE)
  @Override
  public ResponseEntity<ExternalContentImportResult> importSportsDbDayEvents(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
      @RequestParam(required = false) String sport,
      @RequestParam(required = false) String leagueId
  ) {
    log.info("The Sports DB 일별 경기 수동 수집 요청 수신: date={}, sport={}, leagueId={}",
        date, sport, leagueId);
    ExternalContentImportResult response = externalContentManualImportService.importSportsDbDayEvents(
        date,
        sport,
        leagueId
    );
    log.info("The Sports DB 일별 경기 수동 수집 요청 처리 완료: savedCount={}", response.savedCount());

    return ResponseEntity.ok(response);
  }
}
