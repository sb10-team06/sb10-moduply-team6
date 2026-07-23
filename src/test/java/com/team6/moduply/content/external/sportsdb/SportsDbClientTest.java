package com.team6.moduply.content.external.sportsdb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.team6.moduply.content.exception.ContentErrorCode;
import com.team6.moduply.content.exception.ContentException;
import com.team6.moduply.content.external.sportsdb.dto.SportsDbEventListResponse;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class SportsDbClientTest {

  private MockRestServiceServer server;

  private SportsDbClient sportsDbClient;

  private SportsDbProperties properties;

  @BeforeEach
  void setUp() {
    properties = new SportsDbProperties();
    properties.setBaseUrl("https://www.thesportsdb.com/api/v1/json");
    properties.setApiKey("123");

    RestClient.Builder builder = RestClient.builder()
        .baseUrl(properties.getBaseUrl());
    server = MockRestServiceServer.bindTo(builder).build();
    RestClient restClient = builder.build();
    sportsDbClient = new SportsDbClient(restClient, properties);
  }

  @Test
  @DisplayName("The Sports DB 리그 다음 경기 목록 조회에 성공한다.")
  void fetchNextLeagueEvents_success_with_league_id() {
    // Given
    String json = """
        {
          "events": [
            {
              "idEvent": "441613",
              "strEvent": "Arsenal vs Chelsea",
              "strDescriptionEN": "Premier League match.",
              "strThumb": "https://www.thesportsdb.com/images/media/event/thumb.jpg",
              "strSport": "Soccer",
              "strLeague": "English Premier League",
              "dateEvent": "2026-07-01"
            }
          ]
        }
        """;
    server.expect(once(), requestTo(
            "https://www.thesportsdb.com/api/v1/json/123/eventsnextleague.php?id=4328"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

    // When
    SportsDbEventListResponse response = sportsDbClient.fetchNextLeagueEvents("4328");

    // Then
    assertThat(response.events()).hasSize(1);
    assertThat(response.events().get(0).idEvent()).isEqualTo("441613");
    assertThat(response.events().get(0).eventName()).isEqualTo("Arsenal vs Chelsea");
    server.verify();
  }

  @Test
  @DisplayName("The Sports DB 일별 경기 목록 조회에 성공한다.")
  void fetchEventsByDay_success_with_date_and_filters() {
    // Given
    String json = """
        {
          "events": [
            {
              "idEvent": "12345",
              "strEvent": "Los Angeles Dodgers vs San Francisco Giants",
              "strDescriptionEN": "Baseball match.",
              "strThumb": "https://www.thesportsdb.com/images/media/event/baseball.jpg",
              "strSport": "Baseball",
              "strLeague": "MLB",
              "dateEvent": "2026-07-01"
            }
          ]
        }
        """;
    server.expect(once(), requestTo(
            "https://www.thesportsdb.com/api/v1/json/123/eventsday.php?d=2026-07-01&s=Baseball&l=4424"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

    // When
    SportsDbEventListResponse response = sportsDbClient.fetchEventsByDay(
        LocalDate.of(2026, 7, 1),
        "Baseball",
        "4424"
    );

    // Then
    assertThat(response.events()).hasSize(1);
    assertThat(response.events().get(0).sport()).isEqualTo("Baseball");
    assertThat(response.events().get(0).league()).isEqualTo("MLB");
    server.verify();
  }

  @Test
  @DisplayName("The Sports DB 일별 경기 목록 조회 시 날짜만으로 조회할 수 있다.")
  void fetchEventsByDay_success_with_date_only() {
    // Given
    String json = """
        {
          "events": []
        }
        """;
    server.expect(once(), requestTo(
            "https://www.thesportsdb.com/api/v1/json/123/eventsday.php?d=2026-07-01"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

    // When
    SportsDbEventListResponse response = sportsDbClient.fetchEventsByDay(LocalDate.of(2026, 7, 1));

    // Then
    assertThat(response.events()).isEmpty();
    server.verify();
  }

  @Test
  @DisplayName("The Sports DB API 키가 없으면 경기 목록 조회에 실패한다.")
  void fetchNextLeagueEvents_fail_when_api_key_is_blank() {
    // Given
    properties.setApiKey(" ");

    // When & Then
    assertThatThrownBy(() -> sportsDbClient.fetchNextLeagueEvents("4328"))
        .isInstanceOfSatisfying(ContentException.class, exception ->
            assertThat(exception.getErrorCode())
                .isEqualTo(ContentErrorCode.EXTERNAL_CONTENT_CONFIG_INVALID)
        );
  }

  @Test
  @DisplayName("The Sports DB 리그 다음 경기 API 오류 응답이 발생하면 ContentException으로 변환한다.")
  void fetchNextLeagueEvents_fail_when_external_api_error_occurs() {
    // Given
    server.expect(once(), requestTo(
            "https://www.thesportsdb.com/api/v1/json/123/eventsnextleague.php?id=4328"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withServerError());

    // When & Then
    assertThatThrownBy(() -> sportsDbClient.fetchNextLeagueEvents("4328"))
        .isInstanceOfSatisfying(ContentException.class, exception ->
            assertThat(exception.getErrorCode())
                .isEqualTo(ContentErrorCode.EXTERNAL_CONTENT_INVALID_RESPONSE)
        );

    server.verify();
  }

  @Test
  @DisplayName("The Sports DB 일별 경기 API 오류 응답이 발생하면 ContentException으로 변환한다.")
  void fetchEventsByDay_fail_when_external_api_error_occurs() {
    // Given
    server.expect(once(), requestTo(
            "https://www.thesportsdb.com/api/v1/json/123/eventsday.php?d=2026-07-01&s=Baseball&l=4424"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withServerError());

    // When & Then
    assertThatThrownBy(() -> sportsDbClient.fetchEventsByDay(
        LocalDate.of(2026, 7, 1),
        "Baseball",
        "4424"
    )).isInstanceOfSatisfying(ContentException.class, exception ->
        assertThat(exception.getErrorCode())
            .isEqualTo(ContentErrorCode.EXTERNAL_CONTENT_INVALID_RESPONSE)
    );

    server.verify();
  }

  @Test
  @DisplayName("The Sports DB 응답 본문이 없으면 ContentException으로 변환한다.")
  void fetchEventsByDay_fail_when_response_body_is_empty() {
    // Given
    server.expect(once(), requestTo(
            "https://www.thesportsdb.com/api/v1/json/123/eventsday.php?d=2026-07-01"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess("", MediaType.APPLICATION_JSON));

    // When & Then
    assertThatThrownBy(() -> sportsDbClient.fetchEventsByDay(LocalDate.of(2026, 7, 1)))
        .isInstanceOfSatisfying(ContentException.class, exception ->
            assertThat(exception.getErrorCode())
                .isEqualTo(ContentErrorCode.EXTERNAL_CONTENT_INVALID_RESPONSE)
        );

    server.verify();
  }
}
