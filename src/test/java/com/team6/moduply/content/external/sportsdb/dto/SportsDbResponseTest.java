package com.team6.moduply.content.external.sportsdb.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SportsDbResponseTest {

  private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

  @Test
  @DisplayName("The Sports DB 경기 목록 응답을 역직렬화하면 필요한 필드가 유지된다.")
  void deserialize_success_with_event_list_response() throws Exception {
    // Given
    String json = """
        {
          "events": [
            {
              "idEvent": "12345",
              "strEvent": "Arsenal vs Chelsea",
              "strDescriptionEN": "Premier League match.",
              "strThumb": "https://www.thesportsdb.com/images/media/event/thumb.jpg",
              "strSport": "Soccer",
              "strLeague": "English Premier League",
              "dateEvent": "2026-07-01",
              "strTime": "20:00:00"
            }
          ]
        }
        """;

    // When
    SportsDbEventListResponse response = objectMapper.readValue(
        json,
        SportsDbEventListResponse.class
    );

    // Then
    assertThat(response.events()).hasSize(1);

    SportsDbEventResponse event = response.events().get(0);
    assertThat(event.idEvent()).isEqualTo("12345");
    assertThat(event.eventName()).isEqualTo("Arsenal vs Chelsea");
    assertThat(event.description()).isEqualTo("Premier League match.");
    assertThat(event.thumbnailUrl())
        .isEqualTo("https://www.thesportsdb.com/images/media/event/thumb.jpg");
    assertThat(event.sport()).isEqualTo("Soccer");
    assertThat(event.league()).isEqualTo("English Premier League");
    assertThat(event.eventDate()).isEqualTo("2026-07-01");
  }
}
