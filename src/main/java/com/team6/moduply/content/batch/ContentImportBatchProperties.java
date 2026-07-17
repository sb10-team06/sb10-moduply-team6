package com.team6.moduply.content.batch;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "moduply.content.batch.import")
public class ContentImportBatchProperties {

  // 공통 배치 설정
  private boolean enabled;

  @NotBlank
  private String tmdbCron;

  @NotBlank
  private String sportsDbCron;

  // TMDB 일반 수집 설정
  @Min(1)
  private int tmdbPageStart;

  @Min(1)
  private int tmdbPageEnd;

  @NotBlank
  private String tmdbLanguage;

  // Sports DB 일반 수집 설정
  @NotEmpty
  private List<@NotBlank String> sportsDbLeagueIds;

  @NotBlank
  private String sportsDbSport;

  private int sportsDbDayOffsetStart;

  private int sportsDbDayOffsetEnd;

  @Min(0)
  private long sportsDbRequestDelayMillis;

  // 초기 수집 설정
  private boolean initialImportEnabled;

  @Min(1)
  private int initialTmdbPageStart;

  @Min(1)
  private int initialTmdbPageEnd;

  private int initialSportsDbDayOffsetStart;

  private int initialSportsDbDayOffsetEnd;

  @AssertTrue(message = "tmdbPageEnd는 tmdbPageStart보다 크거나 같아야 합니다.")
  public boolean isValidTmdbPageRange() {
    return tmdbPageEnd >= tmdbPageStart;
  }

  @AssertTrue(message = "initialTmdbPageEnd는 initialTmdbPageStart보다 크거나 같아야 합니다.")
  public boolean isValidInitialTmdbPageRange() {
    return initialTmdbPageEnd >= initialTmdbPageStart;
  }

  @AssertTrue(message = "sportsDbDayOffsetEnd는 sportsDbDayOffsetStart보다 크거나 같아야 합니다.")
  public boolean isValidSportsDbDayOffsetRange() {
    return sportsDbDayOffsetEnd >= sportsDbDayOffsetStart;
  }

  @AssertTrue(message = "initialSportsDbDayOffsetEnd는 initialSportsDbDayOffsetStart보다 크거나 같아야 합니다.")
  public boolean isValidInitialSportsDbDayOffsetRange() {
    return initialSportsDbDayOffsetEnd >= initialSportsDbDayOffsetStart;
  }
}
