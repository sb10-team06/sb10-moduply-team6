package com.team6.moduply.content.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team6.moduply.common.pagination.CursorResponse;
import com.team6.moduply.common.pagination.SortDirection;
import com.team6.moduply.binarycontent.exception.BinaryContentErrorCode;
import com.team6.moduply.binarycontent.exception.BinaryContentException;
import com.team6.moduply.content.dto.ContentCreateRequest;
import com.team6.moduply.content.dto.ContentDto;
import com.team6.moduply.content.dto.ContentFindAllRequest;
import com.team6.moduply.content.enums.ContentSortBy;
import com.team6.moduply.content.enums.ContentType;
import com.team6.moduply.content.exception.ContentErrorCode;
import com.team6.moduply.content.exception.ContentException;
import com.team6.moduply.content.service.ContentService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.team6.moduply.auth.filter.JwtAuthenticationFilter;

@WebMvcTest(
    controllers = ContentController.class,
    // 컨트롤러 slice 테스트는 요청/응답과 검증만 확인한다.
    // JwtAuthenticationFilter는 별도 auth 테스트에서 검증하므로 여기서는 Bean 생성 대상에서 제외한다.
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = JwtAuthenticationFilter.class
    )
)
@AutoConfigureMockMvc(addFilters = false)
class ContentControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private ContentService contentService;

  @Test
  @DisplayName("콘텐츠 생성 API 요청 시 응답을 반환한다.")
  void create_success_with_valid_request() throws Exception {
    // Given
    ContentCreateRequest request = new ContentCreateRequest(
        ContentType.movie,
        "Inception",
        "꿈과 현실을 넘나드는 SF 영화",
        List.of("SF", "액션")
    );
    ContentDto response = new ContentDto(
        UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6"),
        ContentType.movie,
        "Inception",
        "꿈과 현실을 넘나드는 SF 영화",
        null,
        List.of("SF", "액션"),
        BigDecimal.ZERO,
        0,
        0L
    );
    MockMultipartFile requestPart = new MockMultipartFile(
        "request",
        "",
        MediaType.APPLICATION_JSON_VALUE,
        objectMapper.writeValueAsBytes(request)
    );
    MockMultipartFile thumbnail = new MockMultipartFile(
        "thumbnail",
        "thumbnail.jpg",
        MediaType.IMAGE_JPEG_VALUE,
        "thumbnail".getBytes()
    );

    given(contentService.create(any(ContentCreateRequest.class), any()))
        .willReturn(response);

    // When & Then
    mockMvc.perform(multipart("/api/contents")
            .file(requestPart)
            .file(thumbnail)
            .with(user("admin").roles("ADMIN"))
            .with(csrf())
            .contentType(MediaType.MULTIPART_FORM_DATA))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(response.id().toString()))
        .andExpect(jsonPath("$.type").value("movie"))
        .andExpect(jsonPath("$.title").value(response.title()))
        .andExpect(jsonPath("$.description").value(response.description()))
        .andExpect(jsonPath("$.thumbnailUrl").doesNotExist())
        .andExpect(jsonPath("$.tags[0]").value("SF"))
        .andExpect(jsonPath("$.tags[1]").value("액션"))
        .andExpect(jsonPath("$.averageRating").value(0))
        .andExpect(jsonPath("$.reviewCount").value(0))
        .andExpect(jsonPath("$.watcherCount").value(0));

    verify(contentService).create(any(ContentCreateRequest.class), any());
  }

  @Test
  @DisplayName("지원하지 않는 썸네일 파일 형식이면 콘텐츠 생성 API 요청에 실패한다.")
  void create_fail_when_thumbnail_type_is_unsupported() throws Exception {
    // Given
    ContentCreateRequest request = new ContentCreateRequest(
        ContentType.movie,
        "Inception",
        "꿈과 현실을 넘나드는 SF 영화",
        List.of("SF", "액션")
    );
    MockMultipartFile requestPart = new MockMultipartFile(
        "request",
        "",
        MediaType.APPLICATION_JSON_VALUE,
        objectMapper.writeValueAsBytes(request)
    );
    MockMultipartFile thumbnail = new MockMultipartFile(
        "thumbnail",
        "thumbnail.gif",
        MediaType.IMAGE_GIF_VALUE,
        "thumbnail".getBytes()
    );

    given(contentService.create(any(ContentCreateRequest.class), any()))
        .willThrow(new BinaryContentException(
            BinaryContentErrorCode.UNSUPPORTED_IMAGE_TYPE,
            Map.of("contentType", MediaType.IMAGE_GIF_VALUE)
        ));

    // When & Then
    mockMvc.perform(multipart("/api/contents")
            .file(requestPart)
            .file(thumbnail)
            .with(user("admin").roles("ADMIN"))
            .with(csrf())
            .contentType(MediaType.MULTIPART_FORM_DATA))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.exceptionType").value("BinaryContentException"))
        .andExpect(jsonPath("$.message").value(BinaryContentErrorCode.UNSUPPORTED_IMAGE_TYPE.getMessage()));

    verify(contentService).create(any(ContentCreateRequest.class), any());
  }

  @Test
  @DisplayName("콘텐츠 생성 API 요청 값이 유효하지 않으면 요청에 실패한다.")
  void create_fail_when_request_is_invalid() throws Exception {
    // Given
    ContentCreateRequest request = new ContentCreateRequest(
        ContentType.movie,
        "",
        "꿈과 현실을 넘나드는 SF 영화",
        List.of("SF", "액션")
    );
    MockMultipartFile requestPart = new MockMultipartFile(
        "request",
        "",
        MediaType.APPLICATION_JSON_VALUE,
        objectMapper.writeValueAsBytes(request)
    );
    MockMultipartFile thumbnail = new MockMultipartFile(
        "thumbnail",
        "thumbnail.jpg",
        MediaType.IMAGE_JPEG_VALUE,
        "thumbnail".getBytes()
    );

    // When & Then
    mockMvc.perform(multipart("/api/contents")
            .file(requestPart)
            .file(thumbnail)
            .with(user("admin").roles("ADMIN"))
            .with(csrf())
            .contentType(MediaType.MULTIPART_FORM_DATA))
        .andExpect(status().isBadRequest());

    verify(contentService, never()).create(
        any(ContentCreateRequest.class),
        any()
    );
  }

  @Test
  @DisplayName("콘텐츠 목록 조회 API를 호출하면 콘텐츠 목록 응답을 반환한다.")
  void find_all_success_with_existing_contents() throws Exception {
    // Given
    ContentDto content = new ContentDto(
        UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6"),
        ContentType.movie,
        "Inception",
        "꿈과 현실을 넘나드는 SF 영화",
        null,
        List.of("SF", "액션"),
        BigDecimal.ZERO,
        0,
        0L
    );
    CursorResponse<ContentDto> response = new CursorResponse<>(
        List.of(content),
        null,
        null,
        false,
        1,
        "createdAt",
        SortDirection.DESCENDING
    );
    ContentFindAllRequest request = new ContentFindAllRequest(
        ContentType.movie,
        "dream",
        List.of("SF", "액션"),
        null,
        null,
        20,
        ContentSortBy.createdAt,
        SortDirection.DESCENDING
    );

    given(contentService.findAll(eq(request))).willReturn(response);

    // When & Then
    mockMvc.perform(get("/api/contents")
            .param("typeEqual", "movie")
            .param("keywordLike", "dream")
            .param("tagsIn", "SF", "액션")
            .param("limit", "20")
            .param("sortBy", "createdAt")
            .param("sortDirection", "DESCENDING")
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].id").value(content.id().toString()))
        .andExpect(jsonPath("$.data[0].type").value("movie"))
        .andExpect(jsonPath("$.data[0].title").value(content.title()))
        .andExpect(jsonPath("$.data[0].description").value(content.description()))
        .andExpect(jsonPath("$.data[0].thumbnailUrl").doesNotExist())
        .andExpect(jsonPath("$.data[0].tags[0]").value("SF"))
        .andExpect(jsonPath("$.data[0].tags[1]").value("액션"))
        .andExpect(jsonPath("$.data[0].averageRating").value(0))
        .andExpect(jsonPath("$.data[0].reviewCount").value(0))
        .andExpect(jsonPath("$.data[0].watcherCount").value(0))
        .andExpect(jsonPath("$.hasNext").value(false))
        .andExpect(jsonPath("$.totalCount").value(1))
        .andExpect(jsonPath("$.sortBy").value("createdAt"))
        .andExpect(jsonPath("$.sortDirection").value("DESCENDING"));

    verify(contentService).findAll(request);
  }

  @Test
  @DisplayName("콘텐츠 단건 조회 API를 호출하면 콘텐츠 응답을 반환한다.")
  void find_success_with_existing_content() throws Exception {
    // Given
    UUID contentId = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");
    ContentDto response = new ContentDto(
        contentId,
        ContentType.movie,
        "Inception",
        "꿈과 현실을 넘나드는 SF 영화",
        null,
        List.of("SF", "액션"),
        BigDecimal.ZERO,
        0,
        0L
    );

    given(contentService.find(contentId)).willReturn(response);

    // When & Then
    mockMvc.perform(get("/api/contents/{contentId}", contentId)
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(response.id().toString()))
        .andExpect(jsonPath("$.type").value("movie"))
        .andExpect(jsonPath("$.title").value(response.title()))
        .andExpect(jsonPath("$.description").value(response.description()))
        .andExpect(jsonPath("$.thumbnailUrl").doesNotExist())
        .andExpect(jsonPath("$.tags[0]").value("SF"))
        .andExpect(jsonPath("$.tags[1]").value("액션"))
        .andExpect(jsonPath("$.averageRating").value(0))
        .andExpect(jsonPath("$.reviewCount").value(0))
        .andExpect(jsonPath("$.watcherCount").value(0));

    verify(contentService).find(contentId);
  }

  @Test
  @DisplayName("존재하지 않는 콘텐츠를 단건 조회하면 404 응답을 반환한다.")
  void find_fail_when_content_not_found() throws Exception {
    UUID contentId = UUID.randomUUID();

    given(contentService.find(contentId))
        .willThrow(new ContentException(
            ContentErrorCode.CONTENT_NOT_FOUND,
            Map.of("contentId", contentId)
        ));

    mockMvc.perform(get("/api/contents/{contentId}", contentId))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.exceptionType").value("ContentException"))
        .andExpect(jsonPath("$.message").value(ContentErrorCode.CONTENT_NOT_FOUND.getMessage()));
  }
}
