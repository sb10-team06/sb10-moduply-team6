package com.team6.moduply.content.external;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.team6.moduply.content.entity.Content;
import com.team6.moduply.content.entity.ContentTag;
import com.team6.moduply.content.entity.Tag;
import com.team6.moduply.content.enums.ContentType;
import com.team6.moduply.content.exception.ContentErrorCode;
import com.team6.moduply.content.exception.ContentException;
import com.team6.moduply.content.external.tmdb.TmdbProperties;
import com.team6.moduply.content.external.tmdb.dto.TmdbMovieResponse;
import com.team6.moduply.content.repository.ContentRepository;
import com.team6.moduply.content.repository.ContentTagRepository;
import com.team6.moduply.content.repository.TagRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExternalContentServiceTest {

  @Mock
  private ContentRepository contentRepository;

  @Mock
  private TagRepository tagRepository;

  @Mock
  private ContentTagRepository contentTagRepository;

  private ExternalContentService externalContentService;

  @BeforeEach
  void setUp() {
    externalContentService = new ExternalContentService(
        contentRepository,
        tagRepository,
        contentTagRepository,
        new ExternalContentMapper(createTmdbProperties())
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
  @DisplayName("외부 콘텐츠 응답이 비어 있으면 저장 작업을 수행하지 않는다.")
  void importTmdbMovies_success_with_empty_response() {
    // When
    ExternalContentImportResult result = externalContentService.importTmdbMovies(List.of());

    // Then
    assertThat(result.requestedCount()).isZero();
    assertThat(result.savedCount()).isZero();
    assertThat(result.skippedCount()).isZero();
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

  private static TmdbProperties createTmdbProperties() {
    TmdbProperties properties = new TmdbProperties();
    properties.setImageBaseUrl("https://image.tmdb.org/t/p");
    return properties;
  }
}
