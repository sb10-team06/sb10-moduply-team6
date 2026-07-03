package com.team6.moduply.content.external.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.team6.moduply.binarycontent.entity.BinaryContent;
import com.team6.moduply.binarycontent.service.BinaryContentService;
import com.team6.moduply.content.entity.Content;
import com.team6.moduply.content.entity.ContentTag;
import com.team6.moduply.content.entity.Tag;
import com.team6.moduply.content.enums.ContentType;
import com.team6.moduply.content.exception.ContentErrorCode;
import com.team6.moduply.content.exception.ContentException;
import com.team6.moduply.content.external.dto.ExternalContentImportResult;
import com.team6.moduply.content.external.dto.ExternalImageFile;
import com.team6.moduply.content.external.image.ExternalImageClient;
import com.team6.moduply.content.external.mapper.ExternalContentMapper;
import com.team6.moduply.content.external.tmdb.TmdbProperties;
import com.team6.moduply.content.external.tmdb.dto.TmdbMovieResponse;
import com.team6.moduply.content.repository.ContentRepository;
import com.team6.moduply.content.repository.ContentTagRepository;
import com.team6.moduply.content.repository.TagRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(MockitoExtension.class)
class ExternalContentServiceTest {

  @Mock
  private ContentRepository contentRepository;

  @Mock
  private TagRepository tagRepository;

  @Mock
  private ContentTagRepository contentTagRepository;

  @Mock
  private ExternalImageClient externalImageClient;

  @Mock
  private BinaryContentService binaryContentService;

  @Mock
  private TransactionTemplate transactionTemplate;

  private ExternalContentService externalContentService;

  @BeforeEach
  void setUp() {
    lenient().when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
      TransactionCallback<?> callback = invocation.getArgument(0);
      return callback.doInTransaction(null);
    });

    externalContentService = new ExternalContentService(
        contentRepository,
        tagRepository,
        contentTagRepository,
        new ExternalContentMapper(createTmdbProperties()),
        externalImageClient,
        binaryContentService,
        transactionTemplate
    );
  }

  @Test
  @DisplayName("TMDB 영화 수집 시 이미 저장된 외부 콘텐츠를 제외하고 새 콘텐츠만 저장한다.")
  void importTmdbMovies_success_with_new_external_contents_only() {
    // Given
    TmdbMovieResponse existingMovie = new TmdbMovieResponse(
        27205L,
        "Inception",
        "꿈과 현실을 넘나드는 SF 영화",
        null,
        null,
        "2010-07-15"
    );
    TmdbMovieResponse newMovie = new TmdbMovieResponse(
        155L,
        "The Dark Knight",
        "배트맨과 조커의 대결",
        null,
        null,
        "2008-07-18"
    );
    Content existingContent = new Content(
        null,
        "tmdb:movie:27205",
        ContentType.movie,
        "Inception",
        "꿈과 현실을 넘나드는 SF 영화"
    );
    Tag tmdbTag = new Tag("TMDB");
    Tag movieTag = new Tag("movie");

    given(contentRepository.findAllByExternalApiIdIn(anyCollection()))
        .willReturn(List.of(existingContent));
    given(contentRepository.saveAll(anyList()))
        .willAnswer(invocation -> invocation.getArgument(0));
    given(tagRepository.findAllByTagNameIn(anyCollection()))
        .willReturn(List.of())
        .willReturn(List.of(tmdbTag, movieTag));

    // When
    ExternalContentImportResult result = externalContentService.importTmdbMovies(
        List.of(existingMovie, newMovie)
    );

    // Then
    assertThat(result.requestedCount()).isEqualTo(2);
    assertThat(result.savedCount()).isEqualTo(1);
    assertThat(result.skippedCount()).isEqualTo(1);
    assertThat(result.imageSavedCount()).isZero();
    assertThat(result.imageFailedCount()).isZero();

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<Content>> contentCaptor = ArgumentCaptor.forClass(List.class);
    verify(contentRepository).saveAll(contentCaptor.capture());
    List<Content> savedContents = contentCaptor.getValue();
    assertThat(savedContents).hasSize(1);
    assertThat(savedContents.get(0).getExternalApiId()).isEqualTo("tmdb:movie:155");
    assertThat(savedContents.get(0).getTitle()).isEqualTo("The Dark Knight");
    assertThat(savedContents.get(0).getContentImg()).isNull();

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<ContentTag>> contentTagCaptor = ArgumentCaptor.forClass(List.class);
    verify(contentTagRepository).saveAll(contentTagCaptor.capture());
    assertThat(contentTagCaptor.getValue()).hasSize(2);
  }

  @Test
  @DisplayName("외부 콘텐츠 여러 건 저장 시 태그를 한 번만 조회한다.")
  void importTmdbMovies_success_with_bulk_tag_lookup() {
    // Given
    TmdbMovieResponse firstMovie = new TmdbMovieResponse(
        27205L,
        "Inception",
        "꿈과 현실을 넘나드는 SF 영화",
        null,
        null,
        "2010-07-15"
    );
    TmdbMovieResponse secondMovie = new TmdbMovieResponse(
        155L,
        "The Dark Knight",
        "배트맨과 조커의 대결",
        null,
        null,
        "2008-07-18"
    );
    TmdbMovieResponse thirdMovie = new TmdbMovieResponse(
        603L,
        "The Matrix",
        "가상 현실과 현실의 경계",
        null,
        null,
        "1999-03-31"
    );
    Tag tmdbTag = new Tag("TMDB");
    Tag movieTag = new Tag("movie");

    given(contentRepository.findAllByExternalApiIdIn(anyCollection()))
        .willReturn(List.of());
    given(contentRepository.saveAll(anyList()))
        .willAnswer(invocation -> invocation.getArgument(0));
    given(tagRepository.findAllByTagNameIn(anyCollection()))
        .willReturn(List.of(tmdbTag, movieTag));

    // When
    ExternalContentImportResult result = externalContentService.importTmdbMovies(
        List.of(firstMovie, secondMovie, thirdMovie)
    );

    // Then
    assertThat(result.requestedCount()).isEqualTo(3);
    assertThat(result.savedCount()).isEqualTo(3);
    verify(tagRepository, times(1)).findAllByTagNameIn(anyCollection());

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<ContentTag>> contentTagCaptor = ArgumentCaptor.forClass(List.class);
    verify(contentTagRepository).saveAll(contentTagCaptor.capture());
    assertThat(contentTagCaptor.getValue()).hasSize(6);
  }

  @Test
  @DisplayName("외부 콘텐츠 응답이 비어 있으면 저장 작업을 수행하지 않는다.")
  void importTmdbMovies_success_with_empty_response() {
    // When
    ExternalContentImportResult result = externalContentService.importTmdbMovies(List.of());

    // Then
    assertThat(result.requestedCount()).isZero();
    assertThat(result.savedCount()).isZero();
    assertThat(result.skippedCount()).isZero();
    assertThat(result.imageSavedCount()).isZero();
    assertThat(result.imageFailedCount()).isZero();
    verify(contentRepository, never()).findAllByExternalApiIdIn(anyCollection());
    verify(contentRepository, never()).saveAll(anyList());
    verify(contentTagRepository, never()).saveAll(anyList());
  }

  @Test
  @DisplayName("외부 콘텐츠 필수 값이 누락되면 저장 작업에 실패한다.")
  void importTmdbMovies_fail_when_required_field_is_missing() {
    // Given
    TmdbMovieResponse response = new TmdbMovieResponse(
        null,
        "Inception",
        "overview",
        null,
        null,
        null
    );

    // When & Then
    assertThatThrownBy(() -> externalContentService.importTmdbMovies(List.of(response)))
        .isInstanceOfSatisfying(ContentException.class, exception -> {
          assertThat(exception.getErrorCode())
              .isEqualTo(ContentErrorCode.EXTERNAL_CONTENT_INVALID_RESPONSE);
        });
    verify(contentRepository, never()).saveAll(anyList());
  }

  @Test
  @DisplayName("외부 콘텐츠 이미지 저장에 성공하면 콘텐츠에 BinaryContent를 연결한다.")
  void importTmdbMovies_success_with_external_thumbnail_image() {
    // Given
    TmdbMovieResponse movie = new TmdbMovieResponse(
        27205L,
        "Inception",
        "꿈과 현실을 넘나드는 SF 영화",
        "/poster.jpg",
        null,
        "2010-07-15"
    );
    Tag tmdbTag = new Tag("TMDB");
    Tag movieTag = new Tag("movie");
    ExternalImageFile imageFile = new ExternalImageFile(
        "poster.jpg",
        "image/jpeg",
        "image-bytes".getBytes()
    );
    BinaryContent contentImg = BinaryContent.create(
        "poster.jpg",
        imageFile.size(),
        "image/jpeg",
        "contents/content-id/thumbnail/poster.jpg"
    );

    given(contentRepository.findAllByExternalApiIdIn(anyCollection()))
        .willReturn(List.of());
    given(contentRepository.saveAll(anyList()))
        .willAnswer(invocation -> {
          @SuppressWarnings("unchecked")
          List<Content> contents = invocation.getArgument(0);
          contents.forEach(content -> ReflectionTestUtils.setField(content, "id", UUID.randomUUID()));
          return contents;
        });
    given(externalImageClient.download("https://image.tmdb.org/t/p/w500/poster.jpg"))
        .willReturn(imageFile);
    given(binaryContentService.createContentImage(
        any(UUID.class),
        eq("poster.jpg"),
        eq(imageFile.bytes()),
        eq("image/jpeg"),
        eq(null)
    )).willReturn(contentImg);
    given(tagRepository.findAllByTagNameIn(anyCollection()))
        .willReturn(List.of())
        .willReturn(List.of(tmdbTag, movieTag));

    // When
    ExternalContentImportResult result = externalContentService.importTmdbMovies(List.of(movie));

    // Then
    assertThat(result.savedCount()).isEqualTo(1);
    assertThat(result.imageSavedCount()).isEqualTo(1);
    assertThat(result.imageFailedCount()).isZero();

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<Content>> contentCaptor = ArgumentCaptor.forClass(List.class);
    verify(contentRepository).saveAll(contentCaptor.capture());
    assertThat(contentCaptor.getValue().get(0).getContentImg()).isEqualTo(contentImg);
  }

  @Test
  @DisplayName("외부 콘텐츠 이미지 저장에 실패해도 콘텐츠 저장은 유지하고 실패 개수를 기록한다.")
  void importTmdbMovies_success_when_external_thumbnail_image_save_fails() {
    // Given
    TmdbMovieResponse movie = new TmdbMovieResponse(
        27205L,
        "Inception",
        "꿈과 현실을 넘나드는 SF 영화",
        "/poster.jpg",
        null,
        "2010-07-15"
    );
    Tag tmdbTag = new Tag("TMDB");
    Tag movieTag = new Tag("movie");

    given(contentRepository.findAllByExternalApiIdIn(anyCollection()))
        .willReturn(List.of());
    given(contentRepository.saveAll(anyList()))
        .willAnswer(invocation -> {
          @SuppressWarnings("unchecked")
          List<Content> contents = invocation.getArgument(0);
          contents.forEach(content -> ReflectionTestUtils.setField(content, "id", UUID.randomUUID()));
          return contents;
        });
    willThrow(new RuntimeException("download failed"))
        .given(externalImageClient)
        .download("https://image.tmdb.org/t/p/w500/poster.jpg");
    given(tagRepository.findAllByTagNameIn(anyCollection()))
        .willReturn(List.of())
        .willReturn(List.of(tmdbTag, movieTag));

    // When
    ExternalContentImportResult result = externalContentService.importTmdbMovies(List.of(movie));

    // Then
    assertThat(result.savedCount()).isEqualTo(1);
    assertThat(result.imageSavedCount()).isZero();
    assertThat(result.imageFailedCount()).isEqualTo(1);
    verify(binaryContentService, never()).createContentImage(
        any(UUID.class),
        any(String.class),
        any(byte[].class),
        any(String.class),
        any()
    );
  }

  private static TmdbProperties createTmdbProperties() {
    TmdbProperties properties = new TmdbProperties();
    properties.setImageBaseUrl("https://image.tmdb.org/t/p");
    return properties;
  }
}
