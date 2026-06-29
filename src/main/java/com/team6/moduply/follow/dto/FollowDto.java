package com.team6.moduply.follow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "팔로우 응답")
public record FollowDto(
    @Schema(description = "팔로우 ID")
    UUID id,

    @Schema(description = "팔로우 요청자 ID")
    UUID followerId,

    @Schema(description = "팔로우 대상자 ID")
    UUID followeeId
) {
}
