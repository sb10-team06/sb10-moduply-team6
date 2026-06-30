package com.team6.moduply.content.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team6.moduply.auth.filter.JwtAuthenticationFilter;
import com.team6.moduply.binarycontent.service.BinaryContentService;
import com.team6.moduply.content.dto.ContentCreateRequest;
import com.team6.moduply.content.dto.ContentDto;
import com.team6.moduply.content.dto.ContentUpdateRequest;
import com.team6.moduply.content.entity.Content;
import com.team6.moduply.content.enums.ContentType;
import com.team6.moduply.content.mapper.ContentMapper;
import com.team6.moduply.content.repository.ContentRepository;
import com.team6.moduply.content.repository.ContentTagRepository;
import com.team6.moduply.content.repository.TagRepository;
import com.team6.moduply.content.service.ContentService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.util.ReflectionTestUtils;

@WebMvcTest(
    controllers = ContentController.class,
    // 이 테스트는 HTTP JWT 필터가 아니라 ContentService의 @PreAuthorize를 검증한다.
    // JwtAuthenticationFilter는 별도 auth 테스트에서 검증하므로 Bean 생성 대상에서 제외한다.
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = JwtAuthenticationFilter.class
    )
)
@Import({
    ContentService.class,
    ContentControllerMethodSecurityTest.MethodSecurityTestConfig.class
})
class ContentControllerMethodSecurityTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private ContentRepository contentRepository;

  @MockitoBean
  private TagRepository tagRepository;

  @MockitoBean
  private ContentTagRepository contentTagRepository;

  @MockitoBean
  private ContentMapper contentMapper;

  @MockitoBean
  private BinaryContentService binaryContentService;

  @Test
  @DisplayName("ADMIN 권한으로 콘텐츠 생성 요청 시 201을 반환한다.")
  void create_success_when_requester_is_admin() throws Exception {
    // Given
    ContentCreateRequest request = new ContentCreateRequest(
        ContentType.movie,
        "Inception",
        "꿈과 현실을 넘나드는 SF 영화",
        List.of("SF", "액션")
    );
    ContentDto response = new ContentDto(
        UUID.randomUUID(),
        ContentType.movie,
        "Inception",
        "꿈과 현실을 넘나드는 SF 영화",
        null,
        List.of("SF", "액션"),
        BigDecimal.ZERO,
        0,
        0L
    );

    given(contentRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));
    given(binaryContentService.createContentImage(any(), any(), any())).willReturn(null);
    given(contentMapper.toDto(any(), any(), any())).willReturn(response);

    // When & Then
    mockMvc.perform(multipart("/api/contents")
            .file(contentCreateRequestPart(request))
            .file(thumbnailPart())
            .with(user("admin@example.com").roles("ADMIN"))
            .with(csrf().asHeader())
            .contentType(MediaType.MULTIPART_FORM_DATA))
        .andExpect(status().isCreated());

    verify(contentRepository).save(any());
  }

  @Test
  @DisplayName("USER 권한으로 콘텐츠 생성 요청 시 403을 반환한다.")
  void create_fail_when_requester_is_not_admin() throws Exception {
    // Given
    ContentCreateRequest request = new ContentCreateRequest(
        ContentType.movie,
        "Inception",
        "꿈과 현실을 넘나드는 SF 영화",
        List.of("SF", "액션")
    );

    // When & Then
    mockMvc.perform(multipart("/api/contents")
            .file(contentCreateRequestPart(request))
            .file(thumbnailPart())
            .with(user("user@example.com").roles("USER"))
            .with(csrf().asHeader())
            .contentType(MediaType.MULTIPART_FORM_DATA))
        .andExpect(status().isForbidden());

    verify(contentRepository, never()).save(any());
  }

  @Test
  @DisplayName("인증 없이 콘텐츠 생성 요청 시 401을 반환한다.")
  void create_fail_when_requester_is_anonymous() throws Exception {
    // Given
    ContentCreateRequest request = new ContentCreateRequest(
        ContentType.movie,
        "Inception",
        "꿈과 현실을 넘나드는 SF 영화",
        List.of("SF", "액션")
    );

    // When & Then
    mockMvc.perform(multipart("/api/contents")
            .file(contentCreateRequestPart(request))
            .file(thumbnailPart())
            .with(csrf().asHeader())
            .contentType(MediaType.MULTIPART_FORM_DATA))
        .andExpect(status().isUnauthorized());

    verify(contentRepository, never()).save(any());
  }

  @Test
  @DisplayName("CSRF 토큰 없이 콘텐츠 생성 요청 시 403을 반환한다.")
  void create_fail_without_csrf_token() throws Exception {
    // Given
    ContentCreateRequest request = new ContentCreateRequest(
        ContentType.movie,
        "Inception",
        "꿈과 현실을 넘나드는 SF 영화",
        List.of("SF", "액션")
    );

    // When & Then
    mockMvc.perform(multipart("/api/contents")
            .file(contentCreateRequestPart(request))
            .file(thumbnailPart())
            .with(user("admin@example.com").roles("ADMIN"))
            .contentType(MediaType.MULTIPART_FORM_DATA))
        .andExpect(status().isForbidden());

    verify(contentRepository, never()).save(any());
  }

  @Test
  @DisplayName("ADMIN 권한으로 콘텐츠 수정 요청 시 200을 반환한다.")
  void update_success_when_requester_is_admin() throws Exception {
    // Given
    UUID contentId = UUID.randomUUID();
    ContentUpdateRequest request = new ContentUpdateRequest(
        "Updated Inception",
        "수정된 콘텐츠 설명",
        List.of()
    );
    Content content = new Content(
        null,
        null,
        ContentType.movie,
        "Inception",
        "꿈과 현실을 넘나드는 SF 영화"
    );
    ReflectionTestUtils.setField(content, "id", contentId);
    ContentDto response = new ContentDto(
        contentId,
        ContentType.movie,
        "Updated Inception",
        "수정된 콘텐츠 설명",
        null,
        List.of(),
        BigDecimal.ZERO,
        0,
        0L
    );

    given(contentRepository.findByIdWithContentImg(contentId)).willReturn(Optional.of(content));
    given(contentMapper.toDto(any(), any(), any())).willReturn(response);

    // When & Then
    mockMvc.perform(multipart("/api/contents/{contentId}", contentId)
            .file(contentUpdateRequestPart(request))
            .with(httpRequest -> {
              httpRequest.setMethod("PATCH");
              return httpRequest;
            })
            .with(user("admin@example.com").roles("ADMIN"))
            .with(csrf().asHeader())
            .contentType(MediaType.MULTIPART_FORM_DATA))
        .andExpect(status().isOk());

    verify(contentRepository).findByIdWithContentImg(contentId);
  }

  @Test
  @DisplayName("USER 권한으로 콘텐츠 수정 요청 시 403을 반환한다.")
  void update_fail_when_requester_is_not_admin() throws Exception {
    // Given
    UUID contentId = UUID.randomUUID();
    ContentUpdateRequest request = new ContentUpdateRequest(
        "Updated Inception",
        "수정된 콘텐츠 설명",
        List.of()
    );

    // When & Then
    mockMvc.perform(multipart("/api/contents/{contentId}", contentId)
            .file(contentUpdateRequestPart(request))
            .with(httpRequest -> {
              httpRequest.setMethod("PATCH");
              return httpRequest;
            })
            .with(user("user@example.com").roles("USER"))
            .with(csrf().asHeader())
            .contentType(MediaType.MULTIPART_FORM_DATA))
        .andExpect(status().isForbidden());

    verify(contentRepository, never()).findByIdWithContentImg(any());
  }

  @Test
  @DisplayName("인증 없이 콘텐츠 수정 요청 시 401을 반환한다.")
  void update_fail_when_requester_is_anonymous() throws Exception {
    // Given
    UUID contentId = UUID.randomUUID();
    ContentUpdateRequest request = new ContentUpdateRequest(
        "Updated Inception",
        "수정된 콘텐츠 설명",
        List.of()
    );

    // When & Then
    mockMvc.perform(multipart("/api/contents/{contentId}", contentId)
            .file(contentUpdateRequestPart(request))
            .with(httpRequest -> {
              httpRequest.setMethod("PATCH");
              return httpRequest;
            })
            .with(csrf().asHeader())
            .contentType(MediaType.MULTIPART_FORM_DATA))
        .andExpect(status().isUnauthorized());

    verify(contentRepository, never()).findByIdWithContentImg(any());
  }

  @Test
  @DisplayName("CSRF 토큰 없이 콘텐츠 수정 요청 시 403을 반환한다.")
  void update_fail_without_csrf_token() throws Exception {
    // Given
    UUID contentId = UUID.randomUUID();
    ContentUpdateRequest request = new ContentUpdateRequest(
        "Updated Inception",
        "수정된 콘텐츠 설명",
        List.of()
    );

    // When & Then
    mockMvc.perform(multipart("/api/contents/{contentId}", contentId)
            .file(contentUpdateRequestPart(request))
            .with(httpRequest -> {
              httpRequest.setMethod("PATCH");
              return httpRequest;
            })
            .with(user("admin@example.com").roles("ADMIN"))
            .contentType(MediaType.MULTIPART_FORM_DATA))
        .andExpect(status().isForbidden());

    verify(contentRepository, never()).findByIdWithContentImg(any());
  }

  @Test
  @DisplayName("ADMIN 권한으로 콘텐츠 삭제 요청 시 204를 반환한다.")
  void delete_success_when_requester_is_admin() throws Exception {
    // Given
    UUID contentId = UUID.randomUUID();
    Content content = new Content(
        null,
        null,
        ContentType.movie,
        "Inception",
        "꿈과 현실을 넘나드는 SF 영화"
    );
    ReflectionTestUtils.setField(content, "id", contentId);

    given(contentRepository.findByIdWithContentImg(contentId)).willReturn(Optional.of(content));

    // When & Then
    mockMvc.perform(delete("/api/contents/{contentId}", contentId)
            .with(user("admin@example.com").roles("ADMIN"))
            .with(csrf().asHeader()))
        .andExpect(status().isNoContent());

    verify(contentRepository).findByIdWithContentImg(contentId);
    verify(contentRepository).delete(content);
  }

  @Test
  @DisplayName("USER 권한으로 콘텐츠 삭제 요청 시 403을 반환한다.")
  void delete_fail_when_requester_is_not_admin() throws Exception {
    // Given
    UUID contentId = UUID.randomUUID();

    // When & Then
    mockMvc.perform(delete("/api/contents/{contentId}", contentId)
            .with(user("user@example.com").roles("USER"))
            .with(csrf().asHeader()))
        .andExpect(status().isForbidden());

    verify(contentRepository, never()).findByIdWithContentImg(any());
  }

  @Test
  @DisplayName("인증 없이 콘텐츠 삭제 요청 시 401을 반환한다.")
  void delete_fail_when_requester_is_anonymous() throws Exception {
    // Given
    UUID contentId = UUID.randomUUID();

    // When & Then
    mockMvc.perform(delete("/api/contents/{contentId}", contentId)
            .with(csrf().asHeader()))
        .andExpect(status().isUnauthorized());

    verify(contentRepository, never()).findByIdWithContentImg(any());
  }

  @Test
  @DisplayName("CSRF 토큰 없이 콘텐츠 삭제 요청 시 403을 반환한다.")
  void delete_fail_without_csrf_token() throws Exception {
    // Given
    UUID contentId = UUID.randomUUID();

    // When & Then
    mockMvc.perform(delete("/api/contents/{contentId}", contentId)
            .with(user("admin@example.com").roles("ADMIN")))
        .andExpect(status().isForbidden());

    verify(contentRepository, never()).findByIdWithContentImg(any());
  }

  private MockMultipartFile contentCreateRequestPart(ContentCreateRequest request) throws Exception {
    return new MockMultipartFile(
        "request",
        "",
        MediaType.APPLICATION_JSON_VALUE,
        objectMapper.writeValueAsBytes(request)
    );
  }

  private MockMultipartFile thumbnailPart() {
    return new MockMultipartFile(
        "thumbnail",
        "thumbnail.jpg",
        MediaType.IMAGE_JPEG_VALUE,
        "thumbnail".getBytes()
    );
  }

  private MockMultipartFile contentUpdateRequestPart(ContentUpdateRequest request) throws Exception {
    return new MockMultipartFile(
        "request",
        "",
        MediaType.APPLICATION_JSON_VALUE,
        objectMapper.writeValueAsBytes(request)
    );
  }

  @EnableMethodSecurity
  static class MethodSecurityTestConfig {
  }
}
