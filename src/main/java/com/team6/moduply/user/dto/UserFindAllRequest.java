package com.team6.moduply.user.dto;

import com.team6.moduply.common.pagination.SortDirection;
import com.team6.moduply.user.enums.Role;
import com.team6.moduply.user.enums.UserSortBy;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "사용자 목록 조회 요청")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserFindAllRequest {

  @Schema(description = "이메일", example = "tester")
  private String emailLike;

  @Schema(description = "권한 (현재 미지원, 추후 반영 예정)", example = "USER")
  private Role roleEqual;

  @Schema(description = "계정 잠금 상태 (현재 미지원, 추후 반영 예정)", example = "false")
  private Boolean isLocked;

  @Schema(description = "커서")
  private String cursor;

  @Schema(description = "보조 커서")
  private UUID idAfter;

  @NotNull(message = "limit은 필수입니다.")
  @Min(value = 1, message = "limit은 1 이상이어야 합니다.")
  @Max(value = 100, message = "limit은 100 이하여야 합니다.")
  @Schema(description = "한 번에 가져올 개수", example = "20", requiredMode = Schema.RequiredMode.REQUIRED)
  private Integer limit;

  @NotNull(message = "sortDirection은 필수입니다.")
  @Schema(description = "정렬 방향", example = "DESCENDING", requiredMode = Schema.RequiredMode.REQUIRED)
  private SortDirection sortDirection;

  @NotNull(message = "sortBy는 필수입니다.")
  @Schema(description = "정렬 기준", example = "createdAt", requiredMode = Schema.RequiredMode.REQUIRED)
  private UserSortBy sortBy;

  @AssertTrue(message = "cursor와 idAfter는 함께 전달되어야 합니다.")
  @Schema(hidden = true)
  public boolean isCursorPairValid() {
    return (cursor == null && idAfter == null) || (cursor != null && idAfter != null);
  }
}
