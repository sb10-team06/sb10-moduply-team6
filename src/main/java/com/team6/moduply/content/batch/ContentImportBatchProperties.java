package com.team6.moduply.content.batch;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "moduply.content.batch.import")
public class ContentImportBatchProperties {

  private boolean enabled;
  private String cron;
  private int tmdbPageStart;
  private int tmdbPageEnd;
  private String tmdbLanguage;
  private List<String> sportsDbLeagueIds;
  private String sportsDbSport;
  private List<Integer> sportsDbDayOffsets;
}
