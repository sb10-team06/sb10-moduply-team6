package com.team6.moduply.content.external.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TmdbTvResponse(
    Long id,
    String name,
    String overview,
    @JsonProperty("poster_path")
    String posterPath,
    @JsonProperty("backdrop_path")
    String backdropPath,
    @JsonProperty("first_air_date")
    String firstAirDate
) {
}
