package com.team6.moduply.content.dto;

import com.team6.moduply.content.enums.ContentType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

@Schema(description = "콘텐츠 생성 요청")
public record ContentCreateRequest(
    @Schema(description = "콘텐츠 타입", example = "movie")
    @NotNull(message = "콘텐츠 타입은 필수입니다.")
    ContentType type,

    @Schema(description = "콘텐츠 제목", example = "Inception")
    @NotBlank(message = "콘텐츠 제목은 필수입니다.")
    @Size(max = 255, message = "콘텐츠 제목은 255자 이하여야 합니다.")
    String title,

    @Schema(description = "콘텐츠 설명", example = "꿈과 현실을 넘나드는 SF 영화")
    @NotBlank(message = "콘텐츠 설명은 필수입니다.")
    String description,

    @Schema(description = "콘텐츠 태그 목록", example = "[\"SF\", \"액션\"]")
    List<
        @NotBlank(message = "태그 이름은 비어 있을 수 없습니다.")
        @Size(max = 100, message = "태그 이름은 100자 이하여야 합니다.")
        String
    > tags
) {
}
