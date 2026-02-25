package com.msgr.tickets.storage;

public record PreparedImportFile(
        String bucket,
        String stagingObjectKey,
        String finalObjectKey,
        String originalFileName,
        String contentType,
        long sizeBytes
) {
}
