package com.team6.moduply.content.external.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TmdbMovieResponse(
    Long id,
    String title,
    String overview,
    @JsonProperty("poster_path")
    String posterPath,
    @JsonProperty("backdrop_path")
    String backdropPath,
    @JsonProperty("release_date")
    String releaseDate
) {
}
