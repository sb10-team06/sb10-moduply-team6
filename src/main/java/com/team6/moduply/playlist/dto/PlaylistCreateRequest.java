package com.team6.moduply.playlist.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record PlaylistCreateRequest(

    @NotBlank(message = "제목을 입력해주세요")
    @Schema(description = "플레이리스트 제목")
    String title,

    @NotBlank(message = "설명을 입력해주세요")
    @Schema(description = "플레이리스트 설명")
    String description
) {}
