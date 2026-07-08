package com.team6.moduply.follow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Schema(description = "팔로우 요청")
public record FollowRequest(
    @NotNull
    @Schema(description = "팔로우할 사용자 ID")
    UUID followeeId
) {
}
