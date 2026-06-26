package com.team6.moduply.content.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.team6.moduply.binarycontent.entity.BinaryContent;
import com.team6.moduply.content.dto.ContentCreateRequest;
import com.team6.moduply.content.dto.ContentDto;
import com.team6.moduply.content.entity.Content;
import com.team6.moduply.content.entity.ContentTag;
import com.team6.moduply.content.entity.Tag;
import com.team6.moduply.content.enums.ContentType;
import com.team6.moduply.content.exception.ContentErrorCode;
import com.team6.moduply.content.exception.ContentException;
import com.team6.moduply.content.mapper.ContentMapper;
import com.team6.moduply.content.repository.ContentRepository;
import com.team6.moduply.content.repository.ContentTagRepository;
import com.team6.moduply.content.repository.TagRepository;
import com.team6.moduply.user.enums.Role;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ContentServiceTest {

  @Mock
  private ContentRepository contentRepository;

  @Mock
  private TagRepository tagRepository;

  @Mock
  private ContentTagRepository contentTagRepository;

  @Mock
  private ContentMapper contentMapper;

  @InjectMocks
  private ContentService contentService;

  @Test
  @DisplayName("관리자가 콘텐츠 생성 요청 시 콘텐츠 저장 성공")
  void create_content_success_when_requester_is_admin() {
    // Given
    ContentCreateRequest request = new ContentCreateRequest(
        ContentType.movie,
        "Inception",
        "꿈과 현실을 넘나드는 SF 영화",
        List.of("SF", "액션", "SF ")
    );
    BinaryContent contentImg = BinaryContent.create(
        "thumbnail.png",
        100L,
        "image/png",
        "contents/content-id/images/binary-content-id.png"
    );
    Tag existingTag = new Tag("SF");
    Tag newTag = new Tag("액션");
    ContentDto expected = new ContentDto(
        null,
        ContentType.movie,
        "Inception",
        "꿈과 현실을 넘나드는 SF 영화",
        "https://example.com/thumbnail.jpg",
        List.of("SF", "액션"),
        BigDecimal.ZERO,
        0,
        0L
    );

    given(contentRepository.save(any(Content.class)))
        .willAnswer(invocation -> invocation.getArgument(0));
    given(tagRepository.findAllByTagNameIn(List.of("SF", "액션")))
        .willReturn(List.of(existingTag), List.of(existingTag, newTag));
    given(tagRepository.insertIgnore(any(UUID.class), eq("액션"))).willReturn(1);
    given(contentTagRepository.saveAll(anyList()))
        .willAnswer(invocation -> invocation.getArgument(0));
    given(contentMapper.toDto(
        any(Content.class),
        any(String.class),
        anyList()
    )).willReturn(expected);

    // When
    ContentDto response = contentService.createContent(
        request,
        contentImg,
        "https://example.com/thumbnail.jpg",
        Role.ADMIN
    );

    // Then
    assertThat(response).isEqualTo(expected);

    ArgumentCaptor<Content> contentCaptor = ArgumentCaptor.forClass(Content.class);
    verify(contentRepository).save(contentCaptor.capture());
    Content savedContent = contentCaptor.getValue();
    assertThat(savedContent.getContentImg()).isEqualTo(contentImg);
    assertThat(savedContent.getExternalApiId()).isNull();
    assertThat(savedContent.getType()).isEqualTo(request.type());
    assertThat(savedContent.getTitle()).isEqualTo(request.title());
    assertThat(savedContent.getDescription()).isEqualTo(request.description());

    verify(contentTagRepository).saveAll(argThat(contentTags -> {
      assertThat(contentTags).hasSize(2);
      return true;
    }));
    verify(contentMapper).toDto(savedContent, "https://example.com/thumbnail.jpg", List.of("SF", "액션"));
    verify(tagRepository, times(2)).findAllByTagNameIn(List.of("SF", "액션"));
    verify(tagRepository).insertIgnore(any(UUID.class), eq("액션"));
  }

  @Test
  @DisplayName("모든 태그가 이미 존재하면 신규 태그 저장 없이 콘텐츠 생성 성공")
  void create_content_success_when_all_tags_already_exist() {
    // Given
    ContentCreateRequest request = new ContentCreateRequest(
        ContentType.movie,
        "Inception",
        "꿈과 현실을 넘나드는 SF 영화",
        List.of("SF", "액션")
    );
    Tag sf = new Tag("SF");
    Tag action = new Tag("액션");
    ContentDto expected = new ContentDto(
        null,
        ContentType.movie,
        "Inception",
        "꿈과 현실을 넘나드는 SF 영화",
        null,
        List.of("SF", "액션"),
        BigDecimal.ZERO,
        0,
        0L
    );

    given(contentRepository.save(any(Content.class)))
        .willAnswer(invocation -> invocation.getArgument(0));
    given(tagRepository.findAllByTagNameIn(List.of("SF", "액션")))
        .willReturn(List.of(sf, action));
    given(contentTagRepository.saveAll(anyList()))
        .willAnswer(invocation -> invocation.getArgument(0));
    given(contentMapper.toDto(any(Content.class), any(), anyList()))
        .willReturn(expected);

    // When
    ContentDto response = contentService.createContent(request, null, null, Role.ADMIN);

    // Then
    assertThat(response).isEqualTo(expected);
    verify(tagRepository).findAllByTagNameIn(List.of("SF", "액션"));
    verify(tagRepository, never()).insertIgnore(any(UUID.class), any(String.class));
    verify(contentTagRepository).saveAll(anyList());
  }

  @Test
  @DisplayName("태그 목록이 비어 있거나 생략된 콘텐츠 생성 성공")
  void create_content_success_without_tags() {
    // Given
    ContentCreateRequest request = new ContentCreateRequest(
        ContentType.sport,
        "World Cup",
        "스포츠 콘텐츠",
        null
    );
    ContentDto expected = new ContentDto(
        null,
        ContentType.sport,
        "World Cup",
        "스포츠 콘텐츠",
        null,
        List.of(),
        BigDecimal.ZERO,
        0,
        0L
    );

    given(contentRepository.save(any(Content.class)))
        .willAnswer(invocation -> invocation.getArgument(0));
    given(contentMapper.toDto(any(Content.class), any(), anyList()))
        .willReturn(expected);

    // When
    ContentDto response = contentService.createContent(
        request,
        null,
        null,
        Role.ADMIN
    );

    // Then
    assertThat(response).isEqualTo(expected);
    verify(tagRepository, never()).findAllByTagNameIn(anyList());
    verify(tagRepository, never()).insertIgnore(any(UUID.class), any(String.class));
    verify(contentTagRepository, never()).saveAll(anyList());
    verify(contentMapper).toDto(any(Content.class), any(), argThat(List::isEmpty));
  }

  @Test
  @DisplayName("관리자가 아닌 사용자가 콘텐츠 생성 요청 시 예외 발생")
  void create_content_fail_when_requester_is_not_admin() {
    // Given
    ContentCreateRequest request = new ContentCreateRequest(
        ContentType.movie,
        "Inception",
        "꿈과 현실을 넘나드는 SF 영화",
        List.of("SF")
    );

    // When & Then
    assertThatThrownBy(() -> contentService.createContent(request, null, null, Role.USER))
        .isInstanceOfSatisfying(ContentException.class, exception -> {
          assertThat(exception.getErrorCode()).isEqualTo(ContentErrorCode.CONTENT_CREATE_FORBIDDEN);
          assertThat(exception.getDetails().get("requesterRole")).isEqualTo(Role.USER.name());
        });

    verify(contentRepository, never()).save(any(Content.class));
    verify(tagRepository, never()).findAllByTagNameIn(anyList());
    verify(tagRepository, never()).insertIgnore(any(UUID.class), any(String.class));
    verify(contentTagRepository, never()).saveAll(anyList());
    verify(contentMapper, never()).toDto(any(Content.class), any(), anyList());
  }
}
