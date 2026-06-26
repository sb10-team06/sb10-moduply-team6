package com.team6.moduply.user.dto;

import com.team6.moduply.user.enums.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "사용자 정보")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {

  @Schema(description = "사용자 ID", format = "uuid", requiredMode = Schema.RequiredMode.REQUIRED)
  private UUID id;

  @Schema(description = "사용자 생성 시간", format = "date-time", requiredMode = Schema.RequiredMode.REQUIRED)
  private Instant createdAt;

  @Schema(description = "이메일", requiredMode = Schema.RequiredMode.REQUIRED)
  private String email;

  @Schema(description = "사용자 이름", requiredMode = Schema.RequiredMode.REQUIRED)
  private String name;

  @Schema(description = "프로필 이미지 URL")
  private String profileImageUrl;

  @Schema(description = "사용자 역할", allowableValues = {"USER", "ADMIN"},
      requiredMode = Schema.RequiredMode.REQUIRED)
  private Role role;

  @Schema(description = "계정 잠금 여부", requiredMode = Schema.RequiredMode.REQUIRED)
  private boolean locked;
}
