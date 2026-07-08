package com.team6.moduply.content.external.sportsdb.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SportsDbEventResponse(
    @JsonProperty("idEvent")
    String idEvent,
    @JsonProperty("strEvent")
    String eventName,
    @JsonProperty("strDescriptionEN")
    String description,
    @JsonProperty("strThumb")
    String thumbnailUrl,
    @JsonProperty("strSport")
    String sport,
    @JsonProperty("strLeague")
    String league,
    @JsonProperty("dateEvent")
    String eventDate
) {
}
