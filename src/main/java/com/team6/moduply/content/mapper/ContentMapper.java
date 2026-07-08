package com.team6.moduply.content.mapper;

import com.team6.moduply.content.dto.ContentDto;
import com.team6.moduply.content.entity.Content;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ContentMapper {

  @Mapping(target = "thumbnailUrl", source = "thumbnailUrl")
  @Mapping(target = "tags", source = "tags")
  ContentDto toDto(Content content, String thumbnailUrl, List<String> tags);
}
