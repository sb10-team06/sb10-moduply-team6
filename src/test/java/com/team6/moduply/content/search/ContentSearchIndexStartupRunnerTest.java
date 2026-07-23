package com.team6.moduply.content.search;

import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

import com.team6.moduply.content.search.service.ContentSearchIndexService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ContentSearchIndexStartupRunnerTest {

  @Mock
  private ContentSearchIndexService contentSearchIndexService;

  @InjectMocks
  private ContentSearchIndexStartupRunner contentSearchIndexStartupRunner;

  @Test
  @DisplayName("애플리케이션 준비 완료 시 검색 인덱스가 비어 있으면 재색인을 요청한다.")
  void rebuildIfEmpty_success() {
    // When
    contentSearchIndexStartupRunner.rebuildIfEmpty();

    // Then
    verify(contentSearchIndexService).rebuildAllIfEmpty();
  }

  @Test
  @DisplayName("검색 인덱스 자동 재색인 실패 시 예외를 전파하지 않는다.")
  void rebuildIfEmpty_success_when_rebuild_fails() {
    // Given
    willThrow(new RuntimeException("elasticsearch unavailable"))
        .given(contentSearchIndexService)
        .rebuildAllIfEmpty();

    // When
    contentSearchIndexStartupRunner.rebuildIfEmpty();

    // Then
    verify(contentSearchIndexService).rebuildAllIfEmpty();
  }
}
