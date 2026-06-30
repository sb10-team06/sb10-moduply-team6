package com.team6.moduply.content.service;

import com.team6.moduply.content.dto.ContentDto;
import com.team6.moduply.content.dto.ContentSummary;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ContentQueryService {

  private final ContentService contentService;

  public ContentSummary find(UUID id) {
    ContentDto contentDto = contentService.find(id);
    return new ContentSummary(
        contentDto.id(),
        contentDto.type(),
        contentDto.title(),
        contentDto.description(),
        contentDto.thumbnailUrl(),
        contentDto.tags().toArray(String[]::new),
        contentDto.averageRating().doubleValue(),
        contentDto.reviewCount()
    );
  }

}
