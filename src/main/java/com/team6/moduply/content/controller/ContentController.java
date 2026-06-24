package com.team6.moduply.content.controller;

import com.team6.moduply.content.dto.ContentCreateRequest;
import com.team6.moduply.content.dto.ContentDto;
import com.team6.moduply.content.service.ContentService;
import com.team6.moduply.user.enums.Role;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@Slf4j
@RequestMapping("/api/contents")
@RequiredArgsConstructor
public class ContentController implements ContentApi {

  private final ContentService contentService;

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  @Override
  public ResponseEntity<ContentDto> createContent(
      @Valid @RequestPart("request") ContentCreateRequest request,
      @RequestPart("thumbnail") MultipartFile thumbnail
  ) {
    log.info("콘텐츠 생성 요청 수신: title={}", request.title());
    // TODO: 썸네일 저장/URL 생성 로직 연동 시 contentImg, thumbnailUrl을 서비스에 전달한다.
    // TODO: Spring Security 인가 구조 적용 시 인증 사용자 권한으로 교체한다.
    ContentDto response = contentService.createContent(request, null, null, Role.ADMIN);
    log.info("콘텐츠 생성 요청 처리 완료: contentId={}", response.id());
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }
}
