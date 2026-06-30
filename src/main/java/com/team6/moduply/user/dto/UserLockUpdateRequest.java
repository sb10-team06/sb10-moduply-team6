package com.team6.moduply.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class UserLockUpdateRequest {

  @NotNull
  @Schema(description = "변경할 잠금 상태", requiredMode = Schema.RequiredMode.REQUIRED)
  private Boolean locked;
}
