package com.team6.moduply.content.search;

import com.team6.moduply.content.search.service.ContentSearchIndexService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ContentSearchIndexStartupRunner {

  private final ContentSearchIndexService contentSearchIndexService;

  @EventListener(ApplicationReadyEvent.class)
  @Order(3)
  public void rebuildIfEmpty() {
    try {
      contentSearchIndexService.rebuildAllIfEmpty();
    } catch (RuntimeException e) {
      // 검색 인덱스는 DB 원본 데이터를 보조하는 저장소이므로, 시작 실패로 애플리케이션을 중단하지 않는다.
      log.warn("콘텐츠 검색 인덱스 자동 재색인 실패", e);
    }
  }
}
