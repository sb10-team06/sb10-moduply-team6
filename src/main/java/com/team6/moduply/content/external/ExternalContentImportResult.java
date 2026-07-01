package com.team6.moduply.content.external;

public record ExternalContentImportResult(
    int requestedCount,
    int savedCount,
    int skippedCount
) {
}
