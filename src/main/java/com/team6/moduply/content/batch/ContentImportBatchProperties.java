package com.team6.moduply.content.batch;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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

  private boolean enabled;

  @NotBlank
  private String cron;

  @Min(1)
  private int tmdbPageStart;

  @Min(1)
  private int tmdbPageEnd;

  @NotBlank
  private String tmdbLanguage;

  @NotEmpty
  private List<@NotBlank String> sportsDbLeagueIds;

  @NotBlank
  private String sportsDbSport;

  @NotEmpty
  private List<@NotNull Integer> sportsDbDayOffsets;

  @AssertTrue(message = "tmdbPageEnd는 tmdbPageStart보다 크거나 같아야 합니다.")
  public boolean isValidTmdbPageRange() {
    return tmdbPageEnd >= tmdbPageStart;
  }
}
