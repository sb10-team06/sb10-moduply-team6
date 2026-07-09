package com.team6.moduply.binarycontent.event;

import com.team6.moduply.binarycontent.entity.BinaryContent;
import com.team6.moduply.binarycontent.exception.BinaryContentErrorCode;
import com.team6.moduply.binarycontent.exception.BinaryContentException;
import com.team6.moduply.binarycontent.repository.BinaryContentRepository;
import com.team6.moduply.binarycontent.service.BinaryContentService;
import com.team6.moduply.binarycontent.storage.BinaryContentStorage;
import com.team6.moduply.common.config.AsyncConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class BinaryContentStorageEventListener {

    private final BinaryContentStorage binaryContentStorage;
    private final BinaryContentService binaryContentService;
    private final BinaryContentRepository binaryContentRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async(AsyncConfig.BINARY_CONTENT_TASK_EXECUTOR)
    public void handleBinaryContentStorage(BinaryContentCreatedEvent event) {
        UUID binaryContentId = event.getBinaryContentId();
        BinaryContent binaryContent;

        try {
            binaryContent = binaryContentRepository.findById(binaryContentId)
                    .orElseThrow(() -> new BinaryContentException(
                            BinaryContentErrorCode.BINARY_CONTENT_NOT_FOUND,
                            Map.of("binaryContentId", binaryContentId.toString())));
        } catch (BinaryContentException e) {
            if (e.getErrorCode() == BinaryContentErrorCode.BINARY_CONTENT_NOT_FOUND) {
                log.error("BinaryContent 메타데이터 조회 실패. binaryContentId={}", binaryContentId, e);
                return;
            }

            handleMetadataLookupFailure(binaryContentId, e);
            return;
        } catch (Exception e) {
            handleMetadataLookupFailure(binaryContentId, e);
            return;
        }

        try {
            /// 실제 파일 저장소 업로드
            binaryContentStorage.upload(
                    binaryContent.getStorageKey(),
                    event.getBytes(),
                    binaryContent.getContentType()
            );
            /// binaryContent 상태 SUCCESS로 업데이트
            /// updatesStatusSuccess가 REQUIRES_NEW라서 바로 SUCCESS로 COMMIT
            binaryContentService.updatesStatusSuccessAndPublishDeleteEvent(binaryContentId, event.getOldBinaryContentId(), event.getOldStorageKey());

        } catch (Exception e) {
            log.error("BinaryContent 업로드 실패. binaryContentId={}", binaryContentId, e);
            try {
                binaryContentStorage.delete(binaryContent.getStorageKey());
            } catch (Exception deleteEx) {
                log.error("업로드 실패 보상 삭제 실패. binaryContentId={}, storageKey={}",
                        binaryContentId,
                        binaryContent.getStorageKey(),
                        deleteEx);
            }
            try {
                /// binaryContent 상태 FAIL로 업데이트
                binaryContentService.updatesStatusFail(binaryContentId);
            } catch (Exception statusEx) {
                log.error("FAIL 상태 갱신 실패. binaryContentId={}", binaryContentId, statusEx);
            }
        }
    }

    private void handleMetadataLookupFailure(UUID binaryContentId, Exception e) {
        log.error("BinaryContent 메타데이터 조회 중 예외 발생. binaryContentId={}", binaryContentId, e);
        try {
            binaryContentService.updatesStatusFail(binaryContentId);
        } catch (Exception statusEx) {
            log.error("FAIL 상태 갱신 실패. binaryContentId={}", binaryContentId, statusEx);
        }
    }


    @Async(AsyncConfig.BINARY_CONTENT_TASK_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleBinaryContentDelete(BinaryContentDeletedEvent event) {
        UUID binaryContentId = event.getBinaryContentId();
        String storageKey = event.getStorageKey();

        try {
            binaryContentStorage.delete(event.getStorageKey());
        } catch (Exception e) {
            try {
                binaryContentService.updatesStatusFail(event.getBinaryContentId());
              //FAIL로 업데이트되는게 예외발생될때.
            } catch (Exception statusException) {
                log.error("BinaryContent 삭제 실패 상태 변경 실패. binaryContentId={}",
                        binaryContentId,
                        statusException);
            }
            log.error("BinaryContent 삭제 실패. binaryContentId={}, storageKey={}",
                    binaryContentId,
                    storageKey,
                    e);

            return;
        }

        /// 실제 S3삭제는 성공했지만 updatesStatusDeleted()가 실패되면 updatesStatusFail()가 실행되는걸 방지하고자
        /// S3삭제 실패와 상태 갱신 실패 분리
        try {
            binaryContentService.updatesStatusDeleted(binaryContentId);
            log.info("BinaryContent 삭제 완료. binaryContentId={}, storageKey={}",
                    binaryContentId,
                    storageKey);

        } catch (Exception e) {
            log.error("S3 삭제는 완료됐지만 DELETED 상태 반영에 실패했습니다. binaryContentId={}, storageKey={}",
                    binaryContentId,
                    storageKey,
                    e);
        }
    }

}
