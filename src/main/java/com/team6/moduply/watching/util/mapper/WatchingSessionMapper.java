package com.team6.moduply.watching.util.mapper;

import com.team6.moduply.content.dto.ContentSummary;
import com.team6.moduply.watching.dto.WatchingSessionDto;
import com.team6.moduply.watching.model.WatchingSession;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface WatchingSessionMapper {

  @Mapping(source = "session.id", target = "id")
  @Mapping(source = "content", target = "content")
  WatchingSessionDto toDto(WatchingSession session, ContentSummary content);
}
