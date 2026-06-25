package com.team6.moduply.binarycontent.event;

import com.team6.moduply.binarycontent.entity.BinaryContent;
import com.team6.moduply.binarycontent.exception.BinaryContentErrorCode;
import com.team6.moduply.binarycontent.exception.BinaryContentException;
import com.team6.moduply.binarycontent.repository.BinaryContentRepository;
import com.team6.moduply.binarycontent.s3.S3BinaryContentStorage;
import com.team6.moduply.binarycontent.service.BinaryContentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
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

    private final S3BinaryContentStorage s3BinaryContentStorage;
    private final BinaryContentService binaryContentService;
    private final BinaryContentRepository binaryContentRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    // TODO: 비동기 설정및 이름 설정 필요
    @Async
    public void handleBinaryContentStorage(BinaryContentCreatedEvent event) {
        UUID binaryContentId = event.getBinaryContentId();

        try {
            BinaryContent binaryContent = binaryContentRepository.findById(binaryContentId)
                    .orElseThrow(() -> new BinaryContentException(
                            BinaryContentErrorCode.BINARY_CONTENT_NOT_FOUND,
                            Map.of("binaryContentId", binaryContentId.toString())));

            /// S3 업로드
            s3BinaryContentStorage.upload(
                    binaryContent.getStorageKey(),
                    event.getBytes(),
                    binaryContent.getContentType()
            );
            /// binaryContent 상태 SUCCESS로 업데이트
            /// updatesStatusSuccess가 REQUIRES_NEW라서 바로 SUCCESS로 COMMIT
            binaryContentService.updatesStatusSuccessAndPublishDeleteEvent(binaryContentId, event.getOldBinaryContentId(), event.getOldStorageKey());

        } catch (Exception e) {
            log.error("S3 업로드 실패. binaryContentId={}", binaryContentId, e);
            try {
                /// binaryContent 상태 FAIL로 업데이트
                binaryContentService.updatesStatusFail(binaryContentId);
            } catch (Exception statusEx) {
                log.error("FAIL 상태 갱신 실패. binaryContentId={}", binaryContentId, statusEx);
            }
        }
    }


    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleBinaryContentDelete(BinaryContentDeletedEvent event) {
        UUID binaryContentId = event.getBinaryContentId();
        String storageKey = event.getStorageKey();

        try {
            // S3에서 삭제
            s3BinaryContentStorage.delete(event.getStorageKey());
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
