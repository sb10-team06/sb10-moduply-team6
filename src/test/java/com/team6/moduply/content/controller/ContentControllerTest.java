package com.team6.moduply.content.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team6.moduply.content.dto.ContentCreateRequest;
import com.team6.moduply.content.dto.ContentDto;
import com.team6.moduply.content.enums.ContentType;
import com.team6.moduply.content.service.ContentService;
import com.team6.moduply.user.enums.Role;
import java.math.BigDecimal;
import java.util.List;
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
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
    controllers = ContentController.class,
    excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ANNOTATION, classes = EnableWebSecurity.class)
    }
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
  @DisplayName("콘텐츠 생성 API 요청 시 응답 반환 성공")
  void create_content_api_success() throws Exception {
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
        "https://example.com/thumbnail.jpg",
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

    given(contentService.createContent(any(ContentCreateRequest.class), isNull(), isNull(), eq(Role.ADMIN)))
        .willReturn(response);

    // When & Then
    mockMvc.perform(multipart("/api/contents")
            .file(requestPart)
            .file(thumbnail)
            .contentType(MediaType.MULTIPART_FORM_DATA))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(response.id().toString()))
        .andExpect(jsonPath("$.type").value("movie"))
        .andExpect(jsonPath("$.title").value(response.title()))
        .andExpect(jsonPath("$.description").value(response.description()))
        .andExpect(jsonPath("$.thumbnailUrl").value(response.thumbnailUrl()))
        .andExpect(jsonPath("$.tags[0]").value("SF"))
        .andExpect(jsonPath("$.tags[1]").value("액션"))
        .andExpect(jsonPath("$.averageRating").value(0))
        .andExpect(jsonPath("$.reviewCount").value(0))
        .andExpect(jsonPath("$.watcherCount").value(0));

    verify(contentService).createContent(any(ContentCreateRequest.class), isNull(), isNull(), eq(Role.ADMIN));
  }

  @Test
  @DisplayName("콘텐츠 생성 API 요청 값이 유효하지 않으면 실패")
  void create_content_api_fail_when_request_is_invalid() throws Exception {
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
            .contentType(MediaType.MULTIPART_FORM_DATA))
        .andExpect(status().isBadRequest());

    verify(contentService, never()).createContent(
        any(ContentCreateRequest.class),
        any(),
        any(),
        any()
    );
  }
}
