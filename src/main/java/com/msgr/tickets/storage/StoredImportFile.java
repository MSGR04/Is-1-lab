package com.msgr.tickets.storage;

public record StoredImportFile(
        String fileName,
        String contentType,
        byte[] bytes
) {
}
