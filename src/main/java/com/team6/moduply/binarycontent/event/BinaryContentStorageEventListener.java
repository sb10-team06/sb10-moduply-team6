package com.team6.moduply.binarycontent.event;

import com.team6.moduply.binarycontent.entity.BinaryContent;
import com.team6.moduply.binarycontent.exception.BinaryContentErrorCode;
import com.team6.moduply.binarycontent.exception.BinaryContentException;
import com.team6.moduply.binarycontent.repository.BinaryContentRepository;
import com.team6.moduply.binarycontent.s3.S3BinaryContentStorage;
import com.team6.moduply.binarycontent.service.BinaryContentService;
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

    private final S3BinaryContentStorage s3BinaryContentStorage;
    private final BinaryContentService binaryContentService;
    private final BinaryContentRepository binaryContentRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void hadnleBinaryContentStorage(BinaryContentCreatedEvent event) {
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
            binaryContentService.updatesStatusSuccess(binaryContentId);

        } catch (Exception e) {
            /// binaryContent 상태 FAIL로 업데이트
            binaryContentService.updatesStatusFail(binaryContentId);
            log.error("S3 업로드 실패. binaryContentId={}", binaryContentId, e);

        }
    }

}
