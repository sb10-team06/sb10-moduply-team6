package com.team6.moduply.follow.mapper;

import com.team6.moduply.follow.dto.FollowDto;
import com.team6.moduply.follow.entity.Follow;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface FollowMapper {

  @Mapping(target = "followerId", source = "follower.id")
  @Mapping(target = "followeeId", source = "followee.id")
  FollowDto toDto(Follow follow);
}
