package com.team6.moduply.user.dto;

import com.team6.moduply.user.enums.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class UserRoleUpdateRequest {
  @NotNull
  @Schema(description = "권한", requiredMode = Schema.RequiredMode.REQUIRED)
  private Role role;
}
