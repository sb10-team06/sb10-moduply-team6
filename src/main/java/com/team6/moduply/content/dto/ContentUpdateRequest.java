package com.team6.moduply.content.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

@Schema(description = "콘텐츠 수정 요청")
public record ContentUpdateRequest(
    @Schema(description = "콘텐츠 제목", example = "Inception")
    @Size(max = 255, message = "콘텐츠 제목은 255자 이하여야 합니다.")
    String title,

    @Schema(description = "콘텐츠 설명", example = "꿈과 현실을 넘나드는 SF 영화")
    String description,

    @Schema(description = "콘텐츠 태그 목록", example = "[\"SF\", \"액션\"]")
    List<
        @NotBlank(message = "태그 이름은 비어 있을 수 없습니다.")
        @Size(max = 100, message = "태그 이름은 100자 이하여야 합니다.")
        String
    > tags
) {
}
