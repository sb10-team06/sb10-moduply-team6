package com.team6.moduply.playlist.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record PlaylistUpdateRequest(

    @Schema(description = "플레이리스트 제목")
    String title,

    @Schema(description = "플레이리스트 설명")
    String description
) {}
