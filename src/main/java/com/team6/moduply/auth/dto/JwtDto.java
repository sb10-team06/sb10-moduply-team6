package com.team6.moduply.auth.dto;

import com.team6.moduply.user.dto.UserDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public class JwtDto {
  private UserDto userDto;
  private String accessToken;
}
