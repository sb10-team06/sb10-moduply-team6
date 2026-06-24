package com.team6.moduply.user.mapper;

import com.team6.moduply.user.dto.UserDto;
import com.team6.moduply.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;


@Mapper(componentModel = "spring")
public interface UserMapper {
  @Mapping(target = "locked", source = "user.blocked")
  // 추후에 profileImg저장하는 로직에서 url을 가져오는 로직이 생기면 수정 예정
  @Mapping(target = "profileImageUrl", source = "profileImgUrl")
  UserDto toDto(User user, String profileImgUrl);

  default UserDto toDto(User user) {
    return toDto(user, null);
  }
}
