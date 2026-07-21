package com.team6.moduply.binarycontent.service;

import com.team6.moduply.binarycontent.entity.BinaryContent;
import com.team6.moduply.binarycontent.event.BinaryContentCreatedEvent;
import com.team6.moduply.binarycontent.event.BinaryContentDeletedEvent;
import com.team6.moduply.binarycontent.exception.BinaryContentErrorCode;
import com.team6.moduply.binarycontent.exception.BinaryContentException;
import com.team6.moduply.binarycontent.repository.BinaryContentRepository;
import com.team6.moduply.binarycontent.storage.BinaryContentStorage;
import com.team6.moduply.common.config.CacheConfig;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 프로필 이미지는 수정 직후 화면 반영이 필요하므로 동기 업로드한다.
 콘텐츠 이미지는 수집/관리 작업에서 사용하므로 기존처럼 이벤트 기반 비동기 업로드를 유지한다.

 ** 기존이미지를 먼저 삭제하면 안됨.
 ** 새 이미지로 변경후, 기존 이미지 삭제할것.

 **/

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class BinaryContentService {

  private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
      "image/jpeg",
      "image/png",
      "image/webp"
  );

  private final BinaryContentStorage binaryContentStorage;
  private final BinaryContentRepository binaryContentRepository;

  private final ApplicationEventPublisher eventPublisher;

  // TODO 사용자 A가 이미지 100번 변경시 이미지 100개 DB저장되는 구조 수정필요
  /// user프로필 이미지 변경
  @Transactional
  public BinaryContent createUserProfile(UUID userId, MultipartFile image, BinaryContent oldProfileImg) throws IOException {
    log.debug("프로필 이미지 생성 시작. userId={}, fileName={}, size={}, contentType={}",
        userId,
        image != null ? image.getOriginalFilename() : null,
        image != null ? image.getSize() : null,
        image != null ? image.getContentType() : null);

    // image null, 빈값, size, 타입 검증
    validateImage(image);

    // image S3 경로값 생성
    String storageKey = createUserProfileStorageKey(userId, image.getOriginalFilename());

    return createUserProfileSync(image, storageKey, oldProfileImg);

  }

  private BinaryContent createUserProfileSync(
      MultipartFile image,
      String storageKey,
      BinaryContent oldProfileImg
  ) throws IOException {
    BinaryContent binaryContent = BinaryContent.create(
        image.getOriginalFilename(),
        image.getSize(),
        image.getContentType(),
        storageKey
    );
    BinaryContent savedBinaryContent = binaryContentRepository.save(binaryContent);

    binaryContentStorage.upload(
        savedBinaryContent.getStorageKey(),
        image.getBytes(),
        savedBinaryContent.getContentType()
    );

    savedBinaryContent.success();

    if (oldProfileImg != null) {
      eventPublisher.publishEvent(new BinaryContentDeletedEvent(
          oldProfileImg.getId(),
          oldProfileImg.getStorageKey()
      ));
    }

    log.info("프로필 이미지 동기 업로드 완료: id={}, fileName={}, size={}, storageKey={}",
        savedBinaryContent.getId(),
        savedBinaryContent.getFileName(),
        savedBinaryContent.getSize(),
        savedBinaryContent.getStorageKey());

    return savedBinaryContent;
  }

  /// 콘텐츠 생성시 썸네일 등록
  @Transactional
  public BinaryContent createContentImage(UUID contentId, MultipartFile image, BinaryContent oldContentImg) throws IOException {
    validateImage(image);
    String storageKey = createContentStorageKey(contentId, image.getOriginalFilename());

    return create(image, storageKey, null, contentId, oldContentImg);

  }

  @Transactional
  public BinaryContent createContentImage(
      UUID contentId,
      String fileName,
      byte[] bytes,
      String contentType,
      BinaryContent oldContentImg
  ) {
    validateImage(fileName, bytes, contentType);
    String storageKey = createContentStorageKey(contentId, fileName);

    return create(fileName, (long) bytes.length, contentType, bytes, storageKey, null, contentId, oldContentImg);
  }

  /// 공통
  private BinaryContent create(
          MultipartFile image,
          String storageKey,
          UUID userId,
          UUID contentId,
          BinaryContent oldBinaryContent
  ) throws IOException {
    return create(
        image.getOriginalFilename(),
        image.getSize(),
        image.getContentType(),
        image.getBytes(),
        storageKey,
        userId,
        contentId,
        oldBinaryContent
    );
  }

  private BinaryContent create(
          String fileName,
          Long size,
          String contentType,
          byte[] bytes,
          String storageKey,
          UUID userId,
          UUID contentId,
          BinaryContent oldBinaryContent
  ) {
    BinaryContent binaryContent = BinaryContent.create(
            fileName,
            size,
            contentType,
            storageKey
    );
    /// 메타데이터 저장
    BinaryContent savedBinaryContent = binaryContentRepository.save(binaryContent);

    /// 기존 binaryContentId 추출
    UUID oldBinaryContentId = oldBinaryContent != null ? oldBinaryContent.getId() : null;

    /// 기존 storageKey 추출
    String oldStorageKey = oldBinaryContent != null ? oldBinaryContent.getStorageKey() : null;

    /// event 발행: S3 비동기 업로드
    eventPublisher.publishEvent(new BinaryContentCreatedEvent(
            savedBinaryContent.getId(),
            bytes,
            userId,
            contentId,
            oldBinaryContentId,
            oldStorageKey
    ));

    log.info("바이너리 컨텐츠 생성 완료: id={}, fileName={}, size={}, storageKey={}",
            savedBinaryContent.getId(),
            savedBinaryContent.getFileName(),
            savedBinaryContent.getSize(),
            savedBinaryContent.getStorageKey());

    return savedBinaryContent;
  }

  @Transactional
  public void delete(UUID binaryContentId) {
    BinaryContent binaryContent = binaryContentRepository.findById(binaryContentId)
            .orElseThrow(() -> new BinaryContentException(
                    BinaryContentErrorCode.BINARY_CONTENT_NOT_FOUND,
                    Map.of("binaryContentId", binaryContentId))
            );

    eventPublisher.publishEvent(new BinaryContentDeletedEvent(
            binaryContent.getId(),
            binaryContent.getStorageKey()
    ));

    log.info("바이너리 컨텐츠 삭제 이벤트 발행 완료. binaryContentId={}, storageKey={}",
            binaryContent.getId(),
            binaryContent.getStorageKey());
  }

  /// 기존 이미지가 있는경우: updatesStatusSuccess -> delete 이벤트 발해
  /// 기존 이미지가 없는경우: updatesStatusSuccess만
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void updatesStatusSuccessAndPublishDeleteEvent(
          UUID binaryContentId,
          UUID oldBinaryContentId,
          String oldStorageKey
  ) {
    BinaryContent binaryContent = binaryContentRepository.findById(binaryContentId)
            .orElseThrow(() -> new BinaryContentException(
                    BinaryContentErrorCode.BINARY_CONTENT_NOT_FOUND,
                    Map.of("binaryContentId", binaryContentId)
            ));

    binaryContent.success();

    if (oldBinaryContentId != null) {
      eventPublisher.publishEvent(
              new BinaryContentDeletedEvent(
                      oldBinaryContentId,
                      oldStorageKey
              )
      );
    }
  }

  /// binaryContent FAIL상태로 업데이트
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void updatesStatusFail(UUID binaryContentId) {
    BinaryContent binaryContent = binaryContentRepository.findById(binaryContentId)
            .orElseThrow(() -> new BinaryContentException(
                    BinaryContentErrorCode.BINARY_CONTENT_NOT_FOUND,
                    Map.of("binaryContentId", binaryContentId))
            );

    binaryContent.fail();
  }

  /// binaryContent DELETED상태로 업데이트
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void updatesStatusDeleted(UUID binaryContentId) {
    BinaryContent binaryContent = binaryContentRepository.findById(binaryContentId)
            .orElseThrow(() -> new BinaryContentException(
                    BinaryContentErrorCode.BINARY_CONTENT_NOT_FOUND,
                    Map.of("binaryContentId", binaryContentId)
            ));

    binaryContent.delete();
  }

  /// 이미지 검증메서드
  private void validateImage(MultipartFile image) {
    /// image null이거나 비어있나 검증
    if (image == null || image.isEmpty()) {
      log.warn("이미지 검증 실패. 파일이 비어 있음");
      throw new BinaryContentException(BinaryContentErrorCode.EMPTY_FILE, Map.of());
    }

    /// 이미지 파일 크기가 0이하인지 검증
    if (image.getSize() <= 0) {
      log.warn("이미지 검증 실패. fileName={}, size={}",
          image.getOriginalFilename(), image.getSize());
      throw new BinaryContentException(
          BinaryContentErrorCode.INVALID_FILE_SIZE,
          Map.of("fileName", image.getOriginalFilename(), "size", image.getSize())
      );
    }

    /// 이미지 파일 타입이 이미지가 아닌다른 타입인지 검증
    if (!ALLOWED_IMAGE_TYPES.contains(image.getContentType())) {
      log.warn("이미지 검증 실패. fileName={}, contentType={}",
          image.getOriginalFilename(), image.getContentType());
      throw new BinaryContentException(
          BinaryContentErrorCode.UNSUPPORTED_IMAGE_TYPE,
          Map.of("fileName", image.getOriginalFilename(), "contentType", image.getContentType())
      );
    }

    log.debug("이미지 검증 완료. fileName={}, size={}, contentType={}",
        image.getOriginalFilename(), image.getSize(), image.getContentType());
  }

  private void validateImage(String fileName, byte[] bytes, String contentType) {
    if (bytes == null || bytes.length == 0) {
      log.warn("이미지 검증 실패. 파일이 비어 있음");
      throw new BinaryContentException(BinaryContentErrorCode.EMPTY_FILE, Map.of());
    }

    if (!ALLOWED_IMAGE_TYPES.contains(contentType)) {
      log.warn("이미지 검증 실패. fileName={}, contentType={}", fileName, contentType);
      throw new BinaryContentException(
          BinaryContentErrorCode.UNSUPPORTED_IMAGE_TYPE,
          Map.of("fileName", fileName, "contentType", contentType)
      );
    }

    log.debug("이미지 검증 완료. fileName={}, size={}, contentType={}",
        fileName, bytes.length, contentType);
  }

  /// user프로필 경로로 image S3 경로값 생성
  private String createUserProfileStorageKey(UUID userId, String fileName) {
    return "users/%s/profile/%s%s".formatted(
        userId,
        UUID.randomUUID(),
        getExtension(fileName)
    );
  }

  /// content 썸네일 경로로 image S3 경로값 생성
  private String createContentStorageKey(UUID contentId, String fileName) {
    return "contents/%s/thumbnail/%s%s".formatted(
            contentId,
            // 동일파일명 중복방지
            UUID.randomUUID(),
            getExtension(fileName)
    );
  }

  /// S3PresignedUrl 생성 메서드.
  /// userService에서 호출 or contentService에서 호출
  @Transactional(readOnly = true)
  @Cacheable(
      cacheNames = CacheConfig.PROFILE_IMAGE_URL,
      key = "#binaryContent.id",
      condition = "#binaryContent != null"
  )
  public String generateUrl(BinaryContent binaryContent) {
    if (binaryContent == null) {
      return null;
    }

    return binaryContentStorage.generateUrl(
            binaryContent.getStorageKey(),
            binaryContent.getContentType()
    );
  }

  private String getExtension(String fileName) {
    if (fileName == null || !fileName.contains(".")) {
      log.debug("파일 확장자 없음. fileName={}", fileName);
      return "";
    }

    return fileName.substring(fileName.lastIndexOf("."));
  }
}
