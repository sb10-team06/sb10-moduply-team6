package com.team6.moduply.content.controller;

import com.team6.moduply.common.pagination.CursorResponse;
import com.team6.moduply.content.dto.ContentCreateRequest;
import com.team6.moduply.content.dto.ContentDto;
import com.team6.moduply.content.dto.ContentFindAllRequest;
import com.team6.moduply.content.dto.ContentUpdateRequest;
import com.team6.moduply.content.service.ContentService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
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
  public ResponseEntity<ContentDto> create(
      @Valid @RequestPart("request") ContentCreateRequest request,
      @RequestPart("thumbnail") MultipartFile thumbnail
  ) {
    log.info("콘텐츠 생성 요청 수신: title={}", request.title());
    ContentDto response = contentService.create(request, thumbnail);
    log.info("콘텐츠 생성 요청 처리 완료: contentId={}", response.id());
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  @Override
  public ResponseEntity<CursorResponse<ContentDto>> findAll(
      @Valid @ModelAttribute ContentFindAllRequest request
  ) {
    log.info("콘텐츠 목록 조회 요청 수신");
    CursorResponse<ContentDto> response = contentService.findAll(request);
    log.info("콘텐츠 목록 조회 요청 처리 완료: count={}", response.data().size());
    return ResponseEntity.ok(response);
  }

  @GetMapping(value = "/{contentId}", produces = MediaType.APPLICATION_JSON_VALUE)
  @Override
  public ResponseEntity<ContentDto> find(@PathVariable UUID contentId) {
    log.info("콘텐츠 단건 조회 요청 수신: contentId={}", contentId);
    ContentDto response = contentService.find(contentId);
    log.info("콘텐츠 단건 조회 요청 처리 완료: contentId={}", response.id());
    return ResponseEntity.ok(response);
  }

  @PatchMapping(
      value = "/{contentId}",
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE
  )
  @Override
  public ResponseEntity<ContentDto> update(
      @PathVariable UUID contentId,
      @Valid @RequestPart("request") ContentUpdateRequest request,
      @RequestPart(value = "thumbnail", required = false) MultipartFile thumbnail
  ) {
    log.info("콘텐츠 수정 요청 수신: contentId={}", contentId);
    ContentDto response = contentService.update(contentId, request, thumbnail);
    log.info("콘텐츠 수정 요청 처리 완료: contentId={}", response.id());
    return ResponseEntity.ok(response);
  }

  @DeleteMapping("/{contentId}")
  @Override
  public ResponseEntity<Void> delete(@PathVariable UUID contentId) {
    log.info("콘텐츠 삭제 요청 수신: contentId={}", contentId);
    contentService.delete(contentId);
    log.info("콘텐츠 삭제 요청 처리 완료: contentId={}", contentId);
    return ResponseEntity.noContent().build();
  }
}
