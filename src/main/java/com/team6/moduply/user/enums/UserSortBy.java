package com.team6.moduply.user.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사용자 정렬 기준")
public enum UserSortBy {
  name, email, createdAt, isLocked, role;
}
