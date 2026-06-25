package com.team6.moduply.binarycontent.service;

import com.team6.moduply.binarycontent.entity.BinaryContent;
import com.team6.moduply.binarycontent.exception.BinaryContentErrorCode;
import com.team6.moduply.binarycontent.exception.BinaryContentException;
import com.team6.moduply.binarycontent.repository.BinaryContentRepository;
import com.team6.moduply.binarycontent.s3.S3BinaryContentStorage;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.team6.moduply.binarycontent.s3.exception.S3ErrorCode;
import com.team6.moduply.binarycontent.s3.exception.S3UploadException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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

  private final S3BinaryContentStorage s3BinaryContentStorage;
  private final BinaryContentRepository binaryContentRepository;

  // TODO 사용자 A가 이미지 100번 변경시 이미지 100개 DB저장되는 구조 수정필요
  /// user프로필 이미지 변경
  public BinaryContent createUserProfile(UUID userId, MultipartFile image) {
    log.debug("프로필 이미지 생성 시작. userId={}, fileName={}, size={}, contentType={}",
        userId,
        image != null ? image.getOriginalFilename() : null,
        image != null ? image.getSize() : null,
        image != null ? image.getContentType() : null);

    // image null, 빈값, size, 타입 검증
    validateImage(image);

    // image S3 경로값 생성
    String storageKey = createUserProfileStorageKey(userId, image.getOriginalFilename());
    log.debug("프로필 이미지 storageKey 생성 완료. userId={}, storageKey={}", userId, storageKey);

    // 메타데이터 생성
    BinaryContent binaryContent = BinaryContent.create(
        image.getOriginalFilename(),
        image.getSize(),
        image.getContentType(),
        storageKey
    );

    try {

      //TODO Spring event 구조로 비동기 적용해서 수정예정
      log.info("S3 프로필 이미지 업로드 시작. userId={}, storageKey={}", userId, storageKey);
      s3BinaryContentStorage.upload(storageKey, image.getBytes(), image.getContentType());

      // binaryContent 상태 SUCCESS로 변경
      binaryContent.success();

      // DB에 저장
      BinaryContent saved = binaryContentRepository.save(binaryContent);
      log.info("프로필 이미지 생성 완료. userId={}, binaryContentId={}, storageKey={}, status={}",
          userId, saved.getId(), saved.getStorageKey(), saved.getStatus());

      // TODO userService에서 생성하도록 수정예정(log로 잘 생성되는지 확인하기위한 용도.)
      // S3Presigned URL 생성
      String presignedUrl = s3BinaryContentStorage.generatePresignedUrl(
          saved.getStorageKey(),
          saved.getContentType()
      );
      log.debug("프로필 이미지 presigned URL 생성 완료. userId={}, binaryContentId={}, urlGenerated={}",
          userId, saved.getId(), presignedUrl != null);

      return saved;
    } catch (IOException | RuntimeException e) {
      binaryContent.fail();
      binaryContentRepository.save(binaryContent);

      log.error("프로필 이미지 업로드 실패. userId={}, storageKey={}, fileName={}", userId, storageKey, image.getOriginalFilename(), e);
      throw new S3UploadException(
              S3ErrorCode.S3_UPLOAD_FAILED,
              Map.of("userId", userId, "storageKey", storageKey),
              e
      );
    }
  }

  /// 콘텐츠 생성시 썸네일 등록
  public BinaryContent createContentImage(UUID contentId, MultipartFile image) {
    validateImage(image);
    String storageKey = createContentStorageKey(contentId, image.getOriginalFilename());

    // 메타데이터 생성
    BinaryContent binaryContent = BinaryContent.create(
            image.getOriginalFilename(),
            image.getSize(),
            image.getContentType(),
            storageKey
    );

    try {

      //TODO Spring event 구조로 비동기 적용해서 수정예정
      log.info("S3 프로필 이미지 업로드 시작. contentId={}, storageKey={}", contentId, storageKey);
      s3BinaryContentStorage.upload(storageKey, image.getBytes(), image.getContentType());

      // binaryContent 상태 SUCCESS로 변경
      binaryContent.success();

      // DB에 저장
      BinaryContent saved = binaryContentRepository.save(binaryContent);
      log.info("프로필 이미지 생성 완료. contentId={}, binaryContentId={}, storageKey={}, status={}",
              contentId, saved.getId(), saved.getStorageKey(), saved.getStatus());

      // TODO contentService에서 생성하도록 수정예정(log로 잘 생성되는지 확인하기위한 용도.)
      // S3Presigned URL 생성
      String presignedUrl = s3BinaryContentStorage.generatePresignedUrl(
              saved.getStorageKey(),
              saved.getContentType()
      );
      log.debug("프로필 이미지 presigned URL 생성 완료. contentId={}, binaryContentId={}, urlGenerated={}",
              contentId, saved.getId(), presignedUrl != null);

      return saved;
    } catch (IOException | RuntimeException e) {
      binaryContent.fail();
      binaryContentRepository.save(binaryContent);

      log.error("프로필 이미지 업로드 실패. contentId={}, storageKey={}, fileName={}", contentId, storageKey, image.getOriginalFilename(), e);
      throw new S3UploadException(
              S3ErrorCode.S3_UPLOAD_FAILED,
              Map.of("contentId", contentId, "storageKey", storageKey),
              e
      );
    }

  }

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
  public String generateUrl(BinaryContent binaryContent) {
    if (binaryContent == null) {
      return null;
    }

    return s3BinaryContentStorage.generatePresignedUrl(
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
