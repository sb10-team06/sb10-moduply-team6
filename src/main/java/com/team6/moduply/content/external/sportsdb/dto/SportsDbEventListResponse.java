package com.team6.moduply.content.external.sportsdb.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SportsDbEventListResponse(
    List<SportsDbEventResponse> events
) {
}
