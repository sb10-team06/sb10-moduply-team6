package com.team6.moduply.content.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.team6.moduply.binarycontent.entity.BinaryContent;
import com.team6.moduply.binarycontent.exception.BinaryContentErrorCode;
import com.team6.moduply.binarycontent.exception.BinaryContentException;
import com.team6.moduply.binarycontent.service.BinaryContentService;
import com.team6.moduply.common.pagination.CursorResponse;
import com.team6.moduply.common.pagination.SortDirection;
import com.team6.moduply.content.dto.ContentCreateRequest;
import com.team6.moduply.content.dto.ContentDetailCacheDto;
import com.team6.moduply.content.dto.ContentDto;
import com.team6.moduply.content.dto.ContentFindAllRequest;
import com.team6.moduply.content.dto.ContentListCacheItemDto;
import com.team6.moduply.content.dto.ContentListCachePageDto;
import com.team6.moduply.content.dto.ContentUpdateRequest;
import com.team6.moduply.content.entity.Content;
import com.team6.moduply.content.entity.ContentTag;
import com.team6.moduply.content.entity.Tag;
import com.team6.moduply.content.enums.ContentSortBy;
import com.team6.moduply.content.enums.ContentType;
import com.team6.moduply.content.exception.ContentErrorCode;
import com.team6.moduply.content.exception.ContentException;
import com.team6.moduply.content.mapper.ContentMapper;
import com.team6.moduply.content.repository.ContentRepository;
import com.team6.moduply.content.repository.ContentTagRepository;
import com.team6.moduply.content.repository.ContentTagRepository.ContentTagNameProjection;
import com.team6.moduply.content.repository.TagRepository;
import com.team6.moduply.review.repository.qdsl.ReviewQDSLRepository;
import com.team6.moduply.review.repository.qdsl.ReviewQDSLRepository.ReviewStats;
import com.team6.moduply.watching.repository.WatchingSessionRepository;
import com.team6.moduply.user.enums.Role;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ContentServiceTest {

  private static final String DEFAULT_THUMBNAIL_URL = "/placeholder-movie.png";

  @Mock
  private ContentRepository contentRepository;

  @Mock
  private TagRepository tagRepository;

  @Mock
  private ContentTagRepository contentTagRepository;

  @Mock
  private ContentMapper contentMapper;

  @Mock
  private BinaryContentService binaryContentService;

  @Mock
  private ContentListCacheService contentListCacheService;

  @Mock
  private ContentDetailCacheService contentDetailCacheService;

  @Mock
  private ReviewQDSLRepository reviewQDSLRepository;

  @Mock
  private WatchingSessionRepository watchingSessionRepository;

  @InjectMocks
  private ContentService contentService;

  @Test
  @DisplayName("관리자가 콘텐츠 생성 요청 시 콘텐츠를 저장한다.")
  void create_success_when_requester_is_admin() throws Exception {
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
    MockMultipartFile thumbnail = new MockMultipartFile(
        "thumbnail",
        "thumbnail.png",
        "image/png",
        "thumbnail".getBytes()
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
    given(binaryContentService.createContentImage(any(), eq(thumbnail), isNull()))
        .willReturn(contentImg);
    given(binaryContentService.generateUrl(contentImg))
        .willReturn("https://example.com/thumbnail.jpg");
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
    ContentDto response = contentService.create(
        request,
        thumbnail
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

    verify(binaryContentService).createContentImage(savedContent.getId(), thumbnail, null);
    verify(binaryContentService).generateUrl(contentImg);
    verify(contentTagRepository).saveAll(argThat(contentTags -> {
      assertThat(contentTags).hasSize(2);
      return true;
    }));
    verify(contentMapper).toDto(
        savedContent,
        "https://example.com/thumbnail.jpg",
        List.of("SF", "액션")
    );
    verify(tagRepository, times(2)).findAllByTagNameIn(List.of("SF", "액션"));
    verify(tagRepository).insertIgnore(any(UUID.class), eq("액션"));
  }

  @Test
  @DisplayName("모든 태그가 이미 존재하면 신규 태그 저장 없이 콘텐츠를 생성한다.")
  void create_success_when_all_tags_already_exist() throws Exception {
    // Given
    ContentCreateRequest request = new ContentCreateRequest(
        ContentType.movie,
        "Inception",
        "꿈과 현실을 넘나드는 SF 영화",
        List.of("SF", "액션")
    );
    Tag sf = new Tag("SF");
    Tag action = new Tag("액션");
    BinaryContent contentImg = BinaryContent.create(
        "thumbnail.png",
        100L,
        "image/png",
        "contents/content-id/images/binary-content-id.png"
    );
    MockMultipartFile thumbnail = new MockMultipartFile(
        "thumbnail",
        "thumbnail.png",
        "image/png",
        "thumbnail".getBytes()
    );
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
    given(binaryContentService.createContentImage(any(), eq(thumbnail), isNull()))
        .willReturn(contentImg);
    given(binaryContentService.generateUrl(contentImg)).willReturn(null);
    given(tagRepository.findAllByTagNameIn(List.of("SF", "액션")))
        .willReturn(List.of(sf, action));
    given(contentTagRepository.saveAll(anyList()))
        .willAnswer(invocation -> invocation.getArgument(0));
    given(contentMapper.toDto(any(Content.class), any(), anyList()))
        .willReturn(expected);

    // When
    ContentDto response = contentService.create(request, thumbnail);

    // Then
    assertThat(response).isEqualTo(expected);
    verify(binaryContentService).createContentImage(any(), eq(thumbnail), isNull());
    verify(binaryContentService).generateUrl(contentImg);
    verify(tagRepository).findAllByTagNameIn(List.of("SF", "액션"));
    verify(tagRepository, never()).insertIgnore(any(UUID.class), any(String.class));
    verify(contentTagRepository).saveAll(anyList());
  }

  @Test
  @DisplayName("태그 목록이 비어 있거나 생략되면 콘텐츠 타입을 기본 태그로 생성한다.")
  void create_success_without_tags() throws Exception {
    // Given
    ContentCreateRequest request = new ContentCreateRequest(
        ContentType.sport,
        "World Cup",
        "스포츠 콘텐츠",
        null
    );
    BinaryContent contentImg = BinaryContent.create(
        "thumbnail.png",
        100L,
        "image/png",
        "contents/content-id/images/binary-content-id.png"
    );
    MockMultipartFile thumbnail = new MockMultipartFile(
        "thumbnail",
        "thumbnail.png",
        "image/png",
        "thumbnail".getBytes()
    );
    ContentDto expected = new ContentDto(
        null,
        ContentType.sport,
        "World Cup",
        "스포츠 콘텐츠",
        null,
        List.of("sport"),
        BigDecimal.ZERO,
        0,
        0L
    );
    Tag sportTag = new Tag("sport");

    given(contentRepository.save(any(Content.class)))
        .willAnswer(invocation -> invocation.getArgument(0));
    given(binaryContentService.createContentImage(any(), eq(thumbnail), isNull()))
        .willReturn(contentImg);
    given(binaryContentService.generateUrl(contentImg)).willReturn(null);
    given(tagRepository.findAllByTagNameIn(List.of("sport")))
        .willReturn(List.of(), List.of(sportTag));
    given(contentMapper.toDto(any(Content.class), any(), anyList()))
        .willReturn(expected);

    // When
    ContentDto response = contentService.create(
        request,
        thumbnail
    );

    // Then
    assertThat(response).isEqualTo(expected);
    verify(binaryContentService).createContentImage(any(), eq(thumbnail), isNull());
    verify(binaryContentService).generateUrl(contentImg);
    verify(tagRepository, times(2)).findAllByTagNameIn(List.of("sport"));
    verify(tagRepository).insertIgnore(any(UUID.class), eq("sport"));
    verify(contentTagRepository).saveAll(argThat(contentTags -> {
      List<ContentTag> contentTagList = new ArrayList<>();
      contentTags.forEach(contentTagList::add);
      assertThat(contentTagList).hasSize(1);
      assertThat(contentTagList.get(0).getTag().getTagName()).isEqualTo("sport");
      return true;
    }));
    verify(contentMapper).toDto(any(Content.class), any(), eq(List.of("sport")));
  }

  @Test
  @DisplayName("썸네일 파일 읽기에 실패하면 콘텐츠 생성에 실패한다.")
  void create_fail_when_thumbnail_read_fails() throws Exception {
    // Given
    ContentCreateRequest request = new ContentCreateRequest(
        ContentType.movie,
        "Inception",
        "꿈과 현실을 넘나드는 SF 영화",
        List.of("SF")
    );
    MockMultipartFile thumbnail = new MockMultipartFile(
        "thumbnail",
        "thumbnail.png",
        "image/png",
        "thumbnail".getBytes()
    );

    given(contentRepository.save(any(Content.class)))
        .willAnswer(invocation -> invocation.getArgument(0));
    given(binaryContentService.createContentImage(any(), eq(thumbnail), isNull()))
        .willThrow(new IOException("read failed"));

    // When & Then
    assertThatThrownBy(() -> contentService.create(request, thumbnail))
        .isInstanceOfSatisfying(BinaryContentException.class, exception -> {
          assertThat(exception.getErrorCode()).isEqualTo(BinaryContentErrorCode.FILE_READ_FAILED);
          assertThat(exception.getDetails().get("fileName")).isEqualTo("thumbnail.png");
        });

    verify(contentTagRepository, never()).saveAll(anyList());
    verify(contentMapper, never()).toDto(any(Content.class), any(), anyList());
  }

  @Test
  @DisplayName("콘텐츠 수정 요청 시 기본 정보와 썸네일 및 태그를 수정한다.")
  void update_success_with_content_fields_thumbnail_and_tags() throws Exception {
    // Given
    UUID contentId = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");
    BinaryContent oldContentImg = BinaryContent.create(
        "old-thumbnail.png",
        100L,
        "image/png",
        "contents/content-id/thumbnail/old-thumbnail.png"
    );
    BinaryContent newContentImg = BinaryContent.create(
        "new-thumbnail.png",
        100L,
        "image/png",
        "contents/content-id/thumbnail/new-thumbnail.png"
    );
    Content content = new Content(
        oldContentImg,
        null,
        ContentType.movie,
        "Old Title",
        "Old Description"
    );
    ReflectionTestUtils.setField(content, "id", contentId);
    ContentUpdateRequest request = new ContentUpdateRequest(
        "Updated Title",
        "Updated Description",
        List.of("SF", "액션", "SF ")
    );
    MockMultipartFile thumbnail = new MockMultipartFile(
        "thumbnail",
        "new-thumbnail.png",
        "image/png",
        "thumbnail".getBytes()
    );
    Tag existingTag = new Tag("SF");
    Tag newTag = new Tag("액션");
    ContentDto expected = new ContentDto(
        contentId,
        ContentType.movie,
        "Updated Title",
        "Updated Description",
        "https://example.com/new-thumbnail.png",
        List.of("SF", "액션"),
        BigDecimal.ZERO,
        0,
        0L
    );

    given(contentRepository.findByIdWithContentImg(contentId)).willReturn(Optional.of(content));
    given(binaryContentService.createContentImage(contentId, thumbnail, oldContentImg))
        .willReturn(newContentImg);
    given(tagRepository.findAllByTagNameIn(List.of("SF", "액션")))
        .willReturn(List.of(existingTag), List.of(existingTag, newTag));
    given(tagRepository.insertIgnore(any(UUID.class), eq("액션"))).willReturn(1);
    given(contentTagRepository.saveAll(anyList()))
        .willAnswer(invocation -> invocation.getArgument(0));
    given(binaryContentService.generateUrl(newContentImg))
        .willReturn("https://example.com/new-thumbnail.png");
    given(contentMapper.toDto(
        content,
        "https://example.com/new-thumbnail.png",
        List.of("SF", "액션")
    )).willReturn(expected);

    // When
    ContentDto response = contentService.update(contentId, request, thumbnail);

    // Then
    assertThat(response).isEqualTo(expected);
    assertThat(content.getTitle()).isEqualTo("Updated Title");
    assertThat(content.getDescription()).isEqualTo("Updated Description");
    assertThat(content.getContentImg()).isEqualTo(newContentImg);
    verify(contentRepository).findByIdWithContentImg(contentId);
    verify(binaryContentService).createContentImage(contentId, thumbnail, oldContentImg);
    verify(contentTagRepository).deleteAllByContentId(contentId);
    verify(contentTagRepository).saveAll(argThat(contentTags -> {
      assertThat(contentTags).hasSize(2);
      return true;
    }));
    verify(tagRepository, times(2)).findAllByTagNameIn(List.of("SF", "액션"));
    verify(tagRepository).insertIgnore(any(UUID.class), eq("액션"));
    verify(binaryContentService).generateUrl(newContentImg);
    verify(contentMapper).toDto(
        content,
        "https://example.com/new-thumbnail.png",
        List.of("SF", "액션")
    );
  }

  @Test
  @DisplayName("일부 필드만 포함된 콘텐츠 수정 요청 시 나머지 값은 유지한다.")
  void update_success_with_partial_request() throws Exception {
    // Given
    UUID contentId = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");
    BinaryContent contentImg = BinaryContent.create(
        "thumbnail.png",
        100L,
        "image/png",
        "contents/content-id/thumbnail/thumbnail.png"
    );
    Content content = new Content(
        contentImg,
        null,
        ContentType.movie,
        "Old Title",
        "Old Description"
    );
    ReflectionTestUtils.setField(content, "id", contentId);
    ContentUpdateRequest request = new ContentUpdateRequest(
        "Updated Title",
        null,
        null
    );
    List<String> existingTagNames = List.of("SF", "액션");
    ContentDto expected = new ContentDto(
        contentId,
        ContentType.movie,
        "Updated Title",
        "Old Description",
        null,
        existingTagNames,
        BigDecimal.ZERO,
        0,
        0L
    );

    given(contentRepository.findByIdWithContentImg(contentId)).willReturn(Optional.of(content));
    given(contentTagRepository.findTagNamesByContentId(contentId)).willReturn(existingTagNames);
    given(binaryContentService.generateUrl(contentImg)).willReturn(null);
    given(contentMapper.toDto(content, null, existingTagNames)).willReturn(expected);

    // When
    ContentDto response = contentService.update(contentId, request, null);

    // Then
    assertThat(response).isEqualTo(expected);
    assertThat(content.getTitle()).isEqualTo("Updated Title");
    assertThat(content.getDescription()).isEqualTo("Old Description");
    assertThat(content.getContentImg()).isEqualTo(contentImg);
    verify(contentTagRepository).findTagNamesByContentId(contentId);
    verify(contentTagRepository, never()).deleteAllByContentId(any(UUID.class));
    verify(contentTagRepository, never()).saveAll(anyList());
    verify(binaryContentService, never()).createContentImage(any(), any(), any());
    verify(binaryContentService).generateUrl(contentImg);
  }

  @Test
  @DisplayName("빈 썸네일 파일이 전달되면 기존 이미지를 유지한다.")
  void update_success_with_empty_thumbnail() throws Exception {
    // Given
    UUID contentId = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");
    BinaryContent contentImg = BinaryContent.create(
        "thumbnail.png",
        100L,
        "image/png",
        "contents/content-id/thumbnail/thumbnail.png"
    );
    Content content = new Content(
        contentImg,
        null,
        ContentType.movie,
        "Old Title",
        "Old Description"
    );
    ReflectionTestUtils.setField(content, "id", contentId);
    ContentUpdateRequest request = new ContentUpdateRequest(
        "Updated Title",
        null,
        null
    );
    MockMultipartFile emptyThumbnail = new MockMultipartFile(
        "thumbnail",
        "thumbnail.png",
        "image/png",
        new byte[0]
    );
    List<String> existingTagNames = List.of("SF", "액션");
    ContentDto expected = new ContentDto(
        contentId,
        ContentType.movie,
        "Updated Title",
        "Old Description",
        null,
        existingTagNames,
        BigDecimal.ZERO,
        0,
        0L
    );

    given(contentRepository.findByIdWithContentImg(contentId)).willReturn(Optional.of(content));
    given(contentTagRepository.findTagNamesByContentId(contentId)).willReturn(existingTagNames);
    given(binaryContentService.generateUrl(contentImg)).willReturn(null);
    given(contentMapper.toDto(content, null, existingTagNames)).willReturn(expected);

    // When
    ContentDto response = contentService.update(contentId, request, emptyThumbnail);

    // Then
    assertThat(response).isEqualTo(expected);
    assertThat(content.getTitle()).isEqualTo("Updated Title");
    assertThat(content.getDescription()).isEqualTo("Old Description");
    assertThat(content.getContentImg()).isEqualTo(contentImg);
    verify(binaryContentService, never()).createContentImage(any(), any(), any());
    verify(binaryContentService).generateUrl(contentImg);
    verify(contentTagRepository).findTagNamesByContentId(contentId);
  }

  @Test
  @DisplayName("태그 목록이 비어 있으면 콘텐츠 태그를 모두 제거한다.")
  void update_success_with_empty_tags() {
    // Given
    UUID contentId = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");
    Content content = new Content(
        null,
        null,
        ContentType.movie,
        "Old Title",
        "Old Description"
    );
    ReflectionTestUtils.setField(content, "id", contentId);
    ContentUpdateRequest request = new ContentUpdateRequest(
        null,
        null,
        List.of()
    );
    ContentDto expected = new ContentDto(
        contentId,
        ContentType.movie,
        "Old Title",
        "Old Description",
        null,
        List.of(),
        BigDecimal.ZERO,
        0,
        0L
    );

    given(contentRepository.findByIdWithContentImg(contentId)).willReturn(Optional.of(content));
    given(contentMapper.toDto(content, DEFAULT_THUMBNAIL_URL, List.of())).willReturn(expected);

    // When
    ContentDto response = contentService.update(contentId, request, null);

    // Then
    assertThat(response).isEqualTo(expected);
    verify(contentTagRepository).deleteAllByContentId(contentId);
    verify(contentTagRepository, never()).saveAll(anyList());
    verify(tagRepository, never()).findAllByTagNameIn(anyList());
  }

  @Test
  @DisplayName("요청 태그가 기존 태그와 동일하면 태그 매핑을 변경하지 않는다.")
  void update_success_without_tag_mapping_changes_when_tags_are_same() {
    // Given
    UUID contentId = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");
    Content content = new Content(
        null,
        null,
        ContentType.movie,
        "Old Title",
        "Old Description"
    );
    ReflectionTestUtils.setField(content, "id", contentId);
    ContentUpdateRequest request = new ContentUpdateRequest(
        null,
        null,
        List.of("액션", "SF", "SF ")
    );
    List<String> existingTagNames = List.of("SF", "액션");
    ContentDto expected = new ContentDto(
        contentId,
        ContentType.movie,
        "Old Title",
        "Old Description",
        "https://example.com/thumbnail.jpg",
        existingTagNames,
        BigDecimal.ZERO,
        0,
        0L
    );

    given(contentRepository.findByIdWithContentImg(contentId)).willReturn(Optional.of(content));
    given(contentTagRepository.findTagNamesByContentId(contentId)).willReturn(existingTagNames);
    given(contentMapper.toDto(content, DEFAULT_THUMBNAIL_URL, existingTagNames)).willReturn(expected);

    // When
    ContentDto response = contentService.update(contentId, request, null);

    // Then
    assertThat(response).isEqualTo(expected);
    verify(contentTagRepository).findTagNamesByContentId(contentId);
    verify(contentTagRepository, never()).deleteAllByContentId(any(UUID.class));
    verify(contentTagRepository, never()).saveAll(anyList());
    verify(tagRepository, never()).findAllByTagNameIn(anyList());
  }

  @Test
  @DisplayName("존재하지 않는 콘텐츠 수정 요청 시 예외를 던진다.")
  void update_fail_when_content_not_found() throws Exception {
    // Given
    UUID contentId = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");
    ContentUpdateRequest request = new ContentUpdateRequest(
        "Updated Title",
        "Updated Description",
        List.of("SF")
    );
    given(contentRepository.findByIdWithContentImg(contentId)).willReturn(Optional.empty());

    // When & Then
    assertThatThrownBy(() -> contentService.update(contentId, request, null))
        .isInstanceOfSatisfying(ContentException.class, exception -> {
          assertThat(exception.getErrorCode()).isEqualTo(ContentErrorCode.CONTENT_NOT_FOUND);
          assertThat(exception.getDetails().get("contentId")).isEqualTo(contentId);
        });

    verify(contentRepository).findByIdWithContentImg(contentId);
    verify(binaryContentService, never()).createContentImage(any(), any(), any());
    verify(contentTagRepository, never()).deleteAllByContentId(any(UUID.class));
    verify(contentMapper, never()).toDto(any(Content.class), any(), anyList());
  }

  @Test
  @DisplayName("썸네일 파일 읽기에 실패하면 콘텐츠 수정에 실패한다.")
  void update_fail_when_thumbnail_read_fails() throws Exception {
    // Given
    UUID contentId = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");
    BinaryContent oldContentImg = BinaryContent.create(
        "old-thumbnail.png",
        100L,
        "image/png",
        "contents/content-id/thumbnail/old-thumbnail.png"
    );
    Content content = new Content(
        oldContentImg,
        null,
        ContentType.movie,
        "Old Title",
        "Old Description"
    );
    ReflectionTestUtils.setField(content, "id", contentId);
    ContentUpdateRequest request = new ContentUpdateRequest(
        "Updated Title",
        "Updated Description",
        List.of("SF")
    );
    MockMultipartFile thumbnail = new MockMultipartFile(
        "thumbnail",
        "new-thumbnail.png",
        "image/png",
        "thumbnail".getBytes()
    );

    given(contentRepository.findByIdWithContentImg(contentId)).willReturn(Optional.of(content));
    given(binaryContentService.createContentImage(contentId, thumbnail, oldContentImg))
        .willThrow(new IOException("read failed"));

    // When & Then
    assertThatThrownBy(() -> contentService.update(contentId, request, thumbnail))
        .isInstanceOfSatisfying(BinaryContentException.class, exception -> {
          assertThat(exception.getErrorCode()).isEqualTo(BinaryContentErrorCode.FILE_READ_FAILED);
          assertThat(exception.getDetails().get("fileName")).isEqualTo("new-thumbnail.png");
        });

    verify(binaryContentService).createContentImage(contentId, thumbnail, oldContentImg);
    verify(contentTagRepository, never()).deleteAllByContentId(any(UUID.class));
    verify(contentMapper, never()).toDto(any(Content.class), any(), anyList());
  }

  @Test
  @DisplayName("콘텐츠 삭제 요청 시 콘텐츠와 태그 매핑을 삭제하고 썸네일 삭제 이벤트를 발행한다.")
  void delete_success_with_existing_content_and_thumbnail() {
    // Given
    UUID contentId = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");
    BinaryContent contentImg = BinaryContent.create(
        "thumbnail.png",
        100L,
        "image/png",
        "contents/content-id/thumbnail/thumbnail.png"
    );
    UUID contentImgId = UUID.fromString("4fa85f64-5717-4562-b3fc-2c963f66afa6");
    ReflectionTestUtils.setField(contentImg, "id", contentImgId);
    Content content = new Content(
        contentImg,
        null,
        ContentType.movie,
        "Inception",
        "꿈과 현실을 넘나드는 SF 영화"
    );
    ReflectionTestUtils.setField(content, "id", contentId);

    given(contentRepository.findByIdWithContentImg(contentId)).willReturn(Optional.of(content));

    // When
    contentService.delete(contentId);

    // Then
    assertThat(content.getContentImg()).isNull();
    verify(contentRepository).findByIdWithContentImg(contentId);
    verify(contentTagRepository).deleteAllByContentId(contentId);
    verify(contentRepository).flush();
    verify(contentRepository).delete(content);
    verify(binaryContentService).delete(contentImgId);

    InOrder inOrder = inOrder(contentTagRepository, contentRepository, binaryContentService);
    inOrder.verify(contentTagRepository).deleteAllByContentId(contentId);
    inOrder.verify(contentRepository).flush();
    inOrder.verify(contentRepository).delete(content);
    inOrder.verify(binaryContentService).delete(contentImgId);
  }

  @Test
  @DisplayName("썸네일이 없는 콘텐츠 삭제 요청 시 콘텐츠와 태그 매핑만 삭제한다.")
  void delete_success_without_thumbnail() {
    // Given
    UUID contentId = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");
    Content content = new Content(
        null,
        null,
        ContentType.movie,
        "Inception",
        "꿈과 현실을 넘나드는 SF 영화"
    );
    ReflectionTestUtils.setField(content, "id", contentId);

    given(contentRepository.findByIdWithContentImg(contentId)).willReturn(Optional.of(content));

    // When
    contentService.delete(contentId);

    // Then
    verify(contentRepository).findByIdWithContentImg(contentId);
    verify(contentTagRepository).deleteAllByContentId(contentId);
    verify(contentRepository, never()).flush();
    verify(contentRepository).delete(content);
    verify(binaryContentService, never()).delete(any(UUID.class));
  }

  @Test
  @DisplayName("존재하지 않는 콘텐츠 삭제 요청 시 예외를 던진다.")
  void delete_fail_when_content_not_found() {
    // Given
    UUID contentId = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");
    given(contentRepository.findByIdWithContentImg(contentId)).willReturn(Optional.empty());

    // When & Then
    assertThatThrownBy(() -> contentService.delete(contentId))
        .isInstanceOfSatisfying(ContentException.class, exception -> {
          assertThat(exception.getErrorCode()).isEqualTo(ContentErrorCode.CONTENT_NOT_FOUND);
          assertThat(exception.getDetails().get("contentId")).isEqualTo(contentId);
        });

    verify(contentRepository).findByIdWithContentImg(contentId);
    verify(contentTagRepository, never()).deleteAllByContentId(any(UUID.class));
    verify(contentRepository, never()).delete(any(Content.class));
    verify(binaryContentService, never()).delete(any(UUID.class));
  }

  @Test
  @DisplayName("콘텐츠 목록이 존재하면 목록 조회 응답을 반환한다.")
  void find_all_success_with_existing_contents() {
    // Given
    UUID movieId = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");
    UUID sportId = UUID.fromString("4fa85f64-5717-4562-b3fc-2c963f66afa6");
    ContentDto movieDto = new ContentDto(
        movieId,
        ContentType.movie,
        "Inception",
        "꿈과 현실을 넘나드는 SF 영화",
        null,
        List.of("SF", "액션"),
        BigDecimal.valueOf(4.50),
        7,
        3L
    );
    ContentDto sportDto = new ContentDto(
        sportId,
        ContentType.sport,
        "World Cup",
        "스포츠 콘텐츠",
        null,
        List.of("스포츠"),
        BigDecimal.valueOf(3.20),
        2,
        5L
    );

    ContentFindAllRequest request = new ContentFindAllRequest(
        null,
        null,
        null,
        null,
        null,
        20,
        ContentSortBy.createdAt,
        SortDirection.DESCENDING
    );
    given(contentListCacheService.findCreatedAtPage(
        null,
        null,
        List.of(),
        null,
        null,
        20,
        ContentSortBy.createdAt,
        SortDirection.DESCENDING
    )).willReturn(new ContentListCachePageDto(
        List.of(
            new ContentListCacheItemDto(
                movieId,
                ContentType.movie,
                "Inception",
                "꿈과 현실을 넘나드는 SF 영화",
                null,
                List.of("SF", "액션"),
                BigDecimal.valueOf(4.50),
                7
            ),
            new ContentListCacheItemDto(
                sportId,
                ContentType.sport,
                "World Cup",
                "스포츠 콘텐츠",
                null,
                List.of("스포츠"),
                BigDecimal.valueOf(3.20),
                2
            )
        ),
        null,
        null,
        false,
        2L,
        "createdAt",
        SortDirection.DESCENDING
    ));
    given(watchingSessionRepository.countByContentIds(List.of(movieId, sportId)))
        .willReturn(Map.of(movieId, 3L, sportId, 5L));

    // When
    CursorResponse<ContentDto> response = contentService.findAll(request);

    // Then
    assertThat(response.data()).containsExactly(movieDto, sportDto);
    assertThat(response.nextCursor()).isNull();
    assertThat(response.nextIdAfter()).isNull();
    assertThat(response.hasNext()).isFalse();
    assertThat(response.totalCount()).isEqualTo(2);
    assertThat(response.sortBy()).isEqualTo("createdAt");
    assertThat(response.sortDirection()).isEqualTo(SortDirection.DESCENDING);
    verify(contentListCacheService).findCreatedAtPage(
        null,
        null,
        List.of(),
        null,
        null,
        20,
        ContentSortBy.createdAt,
        SortDirection.DESCENDING
    );
    verify(watchingSessionRepository).countByContentIds(List.of(movieId, sportId));
    verify(contentRepository, never()).findAllByCursor(any(), any(), anyList(), any(), any(), anyInt(), any(), any());
    verify(contentRepository, never()).countContents(any(), any(), anyList());
    verify(contentTagRepository, never()).findTagNamesByContentIds(anyList());
    verify(contentMapper, never()).toDto(any(Content.class), any(), anyList());
  }

  @Test
  @DisplayName("콘텐츠 목록에 다음 페이지가 있으면 다음 커서 정보를 반환한다.")
  void find_all_success_with_next_cursor() {
    // Given
    UUID firstId = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");
    UUID secondId = UUID.fromString("4fa85f64-5717-4562-b3fc-2c963f66afa6");
    Content first = new Content(
        null,
        null,
        ContentType.movie,
        "Inception",
        "꿈과 현실을 넘나드는 SF 영화"
    );
    Content second = new Content(
        null,
        null,
        ContentType.movie,
        "Interstellar",
        "우주 SF 영화"
    );
    ReflectionTestUtils.setField(first, "id", firstId);
    ReflectionTestUtils.setField(first, "watcherCount", 100L);
    ReflectionTestUtils.setField(second, "id", secondId);
    ReflectionTestUtils.setField(second, "watcherCount", 50L);
    ContentDto firstDto = new ContentDto(
        firstId,
        ContentType.movie,
        "Inception",
        "꿈과 현실을 넘나드는 SF 영화",
        null,
        List.of(),
        BigDecimal.ZERO,
        0,
        100L
    );

    given(contentRepository.findAllByCursor(
        null,
        null,
        List.of(),
        null,
        null,
        2,
        ContentSortBy.watcherCount,
        SortDirection.DESCENDING
    )).willReturn(List.of(first, second));
    given(contentRepository.countContents(null, null, List.of())).willReturn(2L);
    given(contentMapper.toDto(first, DEFAULT_THUMBNAIL_URL, List.of())).willReturn(firstDto);
    given(watchingSessionRepository.countByContentIds(List.of(firstId))).willReturn(Map.of(firstId, 100L));

    ContentFindAllRequest request = new ContentFindAllRequest(
        null,
        null,
        null,
        null,
        null,
        1,
        ContentSortBy.watcherCount,
        SortDirection.DESCENDING
    );

    // When
    CursorResponse<ContentDto> response = contentService.findAll(request);

    // Then
    assertThat(response.data()).containsExactly(firstDto);
    assertThat(response.nextCursor()).isEqualTo("100");
    assertThat(response.nextIdAfter()).isEqualTo(firstId);
    assertThat(response.hasNext()).isTrue();
    assertThat(response.totalCount()).isEqualTo(2);
    assertThat(response.sortBy()).isEqualTo("watcherCount");
    assertThat(response.sortDirection()).isEqualTo(SortDirection.DESCENDING);
    verify(contentRepository).findAllByCursor(
        null,
        null,
        List.of(),
        null,
        null,
        2,
        ContentSortBy.watcherCount,
        SortDirection.DESCENDING
    );
    verify(watchingSessionRepository).countByContentIds(List.of(firstId));
    verify(watchingSessionRepository, never()).countByContentId(firstId);
  }

  @Test
  @DisplayName("콘텐츠 목록이 비어 있으면 빈 목록 조회 응답을 반환한다.")
  void find_all_success_with_empty_contents() {
    // Given
    ContentFindAllRequest request = new ContentFindAllRequest(
        null,
        null,
        null,
        null,
        null,
        20,
        ContentSortBy.createdAt,
        SortDirection.DESCENDING
    );
    given(contentListCacheService.findCreatedAtPage(
        null,
        null,
        List.of(),
        null,
        null,
        20,
        ContentSortBy.createdAt,
        SortDirection.DESCENDING
    )).willReturn(new ContentListCachePageDto(
        List.of(),
        null,
        null,
        false,
        0L,
        "createdAt",
        SortDirection.DESCENDING
    ));

    // When
    CursorResponse<ContentDto> response = contentService.findAll(request);

    // Then
    assertThat(response.data()).isEmpty();
    assertThat(response.hasNext()).isFalse();
    assertThat(response.totalCount()).isZero();
    verify(contentListCacheService).findCreatedAtPage(
        null,
        null,
        List.of(),
        null,
        null,
        20,
        ContentSortBy.createdAt,
        SortDirection.DESCENDING
    );
    verify(watchingSessionRepository, never()).countByContentIds(anyList());
    verify(contentRepository, never()).findAllByCursor(any(), any(), anyList(), any(), any(), anyInt(), any(), any());
    verify(contentRepository, never()).countContents(any(), any(), anyList());
    verify(contentTagRepository, never()).findTagNamesByContentIds(anyList());
    verify(contentMapper, never()).toDto(any(Content.class), any(), anyList());
  }

  @Test
  @DisplayName("콘텐츠가 존재하면 단건 조회 응답을 반환한다.")
  void find_success_with_existing_content() {
    // Given
    UUID contentId = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");
    List<String> tagNames = List.of("SF", "액션");
    ContentDetailCacheDto cachedContent = new ContentDetailCacheDto(
        contentId,
        ContentType.movie,
        "Inception",
        "꿈과 현실을 넘나드는 SF 영화",
        "https://example.com/thumbnail.jpg",
        tagNames,
        BigDecimal.ZERO,
        0
    );
    ContentDto expected = new ContentDto(
        contentId,
        ContentType.movie,
        "Inception",
        "꿈과 현실을 넘나드는 SF 영화",
        "https://example.com/thumbnail.jpg",
        tagNames,
        BigDecimal.ZERO,
        0,
        0L
    );

    given(contentDetailCacheService.find(contentId)).willReturn(cachedContent);

    // When
    ContentDto response = contentService.find(contentId);

    // Then
    assertThat(response).isEqualTo(expected);
    verify(contentDetailCacheService).find(contentId);
    verify(contentRepository, never()).findByIdWithContentImg(contentId);
    verify(contentTagRepository, never()).findTagNamesByContentId(contentId);
    verify(binaryContentService, never()).generateUrl(any(BinaryContent.class));
    verify(contentMapper, never()).toDto(any(Content.class), any(), anyList());
  }

  @Test
  @DisplayName("콘텐츠 단건 조회 시 현재 시청자 수를 조회 응답에 반영한다.")
  void find_success_with_current_watcher_count() {
    // Given
    UUID contentId = UUID.randomUUID();
    List<String> tagNames = List.of("SF", "액션");
    ContentDetailCacheDto cachedContent = new ContentDetailCacheDto(
        contentId,
        ContentType.movie,
        "Inception",
        "꿈과 현실을 넘나드는 SF 영화",
        DEFAULT_THUMBNAIL_URL,
        tagNames,
        BigDecimal.ZERO,
        0
    );

    given(contentDetailCacheService.find(contentId)).willReturn(cachedContent);
    given(watchingSessionRepository.countByContentId(contentId)).willReturn(7L);

    // When
    ContentDto response = contentService.find(contentId);

    // Then
    assertThat(response.watcherCount()).isEqualTo(7L);
    verify(watchingSessionRepository).countByContentId(contentId);
  }

  @Test
  @DisplayName("콘텐츠가 존재하지 않으면 예외를 던진다.")
  void find_fail_when_content_not_found() {
    // Given
    UUID contentId = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");
    given(contentDetailCacheService.find(contentId)).willThrow(new ContentException(
        ContentErrorCode.CONTENT_NOT_FOUND,
        Map.of("contentId", contentId)
    ));

    // When & Then
    assertThatThrownBy(() -> contentService.find(contentId))
        .isInstanceOfSatisfying(ContentException.class, exception -> {
          assertThat(exception.getErrorCode()).isEqualTo(ContentErrorCode.CONTENT_NOT_FOUND);
          assertThat(exception.getDetails().get("contentId")).isEqualTo(contentId);
        });

    verify(contentDetailCacheService).find(contentId);
    verify(contentRepository, never()).findByIdWithContentImg(contentId);
    verify(contentTagRepository, never()).findTagNamesByContentId(any(UUID.class));
    verify(contentMapper, never()).toDto(any(Content.class), any(), anyList());
  }

  @Test
  @DisplayName("리뷰 통계를 재계산하면 콘텐츠 평균 평점과 리뷰 수를 갱신한다.")
  void refresh_review_stats_success() {
    // Given
    UUID contentId = UUID.randomUUID();
    Content content = new Content(null, null, ContentType.movie, "title", "description");
    given(contentRepository.findById(contentId)).willReturn(Optional.of(content));
    given(reviewQDSLRepository.calculateStatsByContentId(contentId))
        .willReturn(new ReviewStats(3L, 4.333));

    // When
    contentService.refreshReviewStats(contentId);

    // Then
    assertThat(content.getReviewCount()).isEqualTo(3);
    assertThat(content.getAverageRating()).isEqualByComparingTo(BigDecimal.valueOf(4.33));
  }

  @Test
  @DisplayName("리뷰 통계 갱신 시 콘텐츠가 존재하지 않으면 예외를 던진다.")
  void refresh_review_stats_fail_when_content_not_found() {
    // Given
    UUID contentId = UUID.randomUUID();
    given(contentRepository.findById(contentId)).willReturn(Optional.empty());

    // When & Then
    assertThatThrownBy(() -> contentService.refreshReviewStats(contentId))
        .isInstanceOfSatisfying(ContentException.class, exception -> {
          assertThat(exception.getErrorCode()).isEqualTo(ContentErrorCode.CONTENT_NOT_FOUND);
          assertThat(exception.getDetails().get("contentId")).isEqualTo(contentId);
        });

    verify(reviewQDSLRepository, never()).calculateStatsByContentId(contentId);
  }

  private record TestContentTagNameProjection(
      UUID contentId,
      String tagName
  ) implements ContentTagNameProjection {

    @Override
    public UUID getContentId() {
      return contentId;
    }

    @Override
    public String getTagName() {
      return tagName;
    }
  }

}
