package com.team6.moduply.content.search.mapper;

import com.team6.moduply.content.entity.Content;
import com.team6.moduply.content.search.document.ContentSearchDocument;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ContentSearchDocumentMapper {

  public ContentSearchDocument toDocument(Content content, List<String> tagNames) {
    return ContentSearchDocument.builder()
        .id(content.getId().toString())
        .externalApiId(content.getExternalApiId())
        .type(content.getType())
        .title(content.getTitle())
        .description(content.getDescription())
        .tags(List.copyOf(tagNames))
        .averageRating(content.getAverageRating())
        .reviewCount(content.getReviewCount())
        .createdAt(content.getCreatedAt())
        .updatedAt(content.getUpdatedAt())
        .build();
  }
}
