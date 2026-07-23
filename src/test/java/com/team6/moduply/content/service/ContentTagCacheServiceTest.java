package com.team6.moduply.content.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.team6.moduply.content.repository.ContentTagRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ContentTagCacheServiceTest {

  @Mock
  private ContentTagRepository contentTagRepository;

  @InjectMocks
  private ContentTagCacheService contentTagCacheService;

  @Test
  @DisplayName("콘텐츠 ID로 태그명을 조회한다.")
  void find_tag_names_by_content_id_success() {
    // Given
    UUID contentId = UUID.randomUUID();
    given(contentTagRepository.findTagNamesByContentId(contentId))
        .willReturn(List.of("SF", "명작"));

    // When
    List<String> result = contentTagCacheService.findTagNamesByContentId(contentId);

    // Then
    assertThat(result).containsExactly("SF", "명작");
    verify(contentTagRepository).findTagNamesByContentId(contentId);
  }
}
