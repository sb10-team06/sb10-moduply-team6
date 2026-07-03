package com.team6.moduply.content.external.sportsdb;

import com.team6.moduply.content.exception.ContentErrorCode;
import com.team6.moduply.content.exception.ContentException;
import com.team6.moduply.content.external.sportsdb.dto.SportsDbEventListResponse;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriBuilder;

@Component
public class SportsDbClient {

  private final RestClient restClient;

  private final SportsDbProperties properties;

  public SportsDbClient(
      @Qualifier("sportsDbRestClient") RestClient restClient,
      SportsDbProperties properties
  ) {
    this.restClient = restClient;
    this.properties = properties;
  }

  public SportsDbEventListResponse fetchNextLeagueEvents(String leagueId) {
    validateApiKey();

    try {
      SportsDbEventListResponse response = restClient.get()
          .uri(uriBuilder -> uriBuilder
              .path(apiPath("eventsnextleague.php"))
              .queryParam("id", leagueId)
              .build())
          .retrieve()
          .body(SportsDbEventListResponse.class);

      return requireResponse(
          response,
          "The Sports DB 리그 다음 경기 응답이 비어 있습니다.",
          "eventsnextleague.php"
      );
    } catch (RestClientException | IllegalArgumentException e) {
      throw externalApiException("eventsnextleague.php", e);
    }
  }

  public SportsDbEventListResponse fetchEventsByDay(LocalDate date) {
    return fetchEventsByDay(date, null, null);
  }

  public SportsDbEventListResponse fetchEventsByDay(
      LocalDate date,
      String sport,
      String leagueId
  ) {
    validateApiKey();

    try {
      SportsDbEventListResponse response = restClient.get()
          .uri(uriBuilder -> {
            UriBuilder builder = uriBuilder
                .path(apiPath("eventsday.php"))
                .queryParam("d", date.format(DateTimeFormatter.ISO_LOCAL_DATE));

            if (StringUtils.hasText(sport)) {
              builder.queryParam("s", sport);
            }

            if (StringUtils.hasText(leagueId)) {
              builder.queryParam("l", leagueId);
            }

            return builder.build();
          })
          .retrieve()
          .body(SportsDbEventListResponse.class);

      return requireResponse(
          response,
          "The Sports DB 일별 경기 응답이 비어 있습니다.",
          "eventsday.php"
      );
    } catch (RestClientException | IllegalArgumentException e) {
      throw externalApiException("eventsday.php", e);
    }
  }

  private String apiPath(String endpoint) {
    return "/%s/%s".formatted(properties.getApiKey(), endpoint);
  }

  private void validateApiKey() {
    if (!StringUtils.hasText(properties.getApiKey())) {
      throw new ContentException(
          ContentErrorCode.EXTERNAL_CONTENT_CONFIG_INVALID,
          Map.of("property", "SPORTS_DB_API_KEY")
      );
    }
  }

  private SportsDbEventListResponse requireResponse(
      SportsDbEventListResponse response,
      String message,
      String endpoint
  ) {
    if (response == null) {
      throw new ContentException(
          ContentErrorCode.EXTERNAL_CONTENT_INVALID_RESPONSE,
          Map.of("provider", "The Sports DB", "endpoint", endpoint, "reason", message)
      );
    }

    return response;
  }

  private ContentException externalApiException(String endpoint, RuntimeException e) {
    return new ContentException(
        ContentErrorCode.EXTERNAL_CONTENT_INVALID_RESPONSE,
        Map.of("provider", "The Sports DB", "endpoint", endpoint),
        e
    );
  }
}
