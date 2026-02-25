package com.msgr.tickets.storage;

import com.msgr.tickets.config.AppConfigService;
import io.minio.BucketExistsArgs;
import io.minio.CopySource;
import io.minio.CopyObjectArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.errors.ErrorResponseException;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.ServiceUnavailableException;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class ImportFileStorageService {

    private static final Logger LOG = Logger.getLogger(ImportFileStorageService.class.getName());
    private static final String DEFAULT_CONTENT_TYPE = "text/csv";

    @Inject
    private AppConfigService config;

    private MinioClient client;
    private String bucket;
    private boolean enabled;
    private boolean autoCreateBucket;

    @PostConstruct
    void init() {
        enabled = config.getBoolean("app.minio.enabled", true);
        bucket = config.getString("app.minio.bucket", "ticket-imports");
        autoCreateBucket = config.getBoolean("app.minio.auto-create-bucket", true);

        if (!enabled) {
            return;
        }

        String endpoint = config.getString("app.minio.endpoint", "http://127.0.0.1:9000");
        String accessKey = config.getString("app.minio.access-key", "minioadmin");
        String secretKey = config.getString("app.minio.secret-key", "minioadmin");

        client = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();

        ensureBucket();
    }

    public PreparedImportFile prepare(long operationId, String originalFileName, String contentType, byte[] bytes) {
        ensureEnabled();
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("file body is empty");
        }

        String safeName = sanitizeFileName(originalFileName);
        String objectSuffix = UUID.randomUUID() + "-" + safeName;
        String stagingObjectKey = "imports-staging/" + operationId + "/" + objectSuffix;
        String finalObjectKey = "imports/" + operationId + "/" + objectSuffix;
        String safeContentType = normalizeContentType(contentType);

        try (InputStream input = new ByteArrayInputStream(bytes)) {
            client.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(stagingObjectKey)
                    .contentType(safeContentType)
                    .stream(input, bytes.length, -1)
                    .build());
        } catch (Exception e) {
            throw new ServiceUnavailableException("Minio не запущен");
        }

        return new PreparedImportFile(
                bucket,
                stagingObjectKey,
                finalObjectKey,
                safeName,
                safeContentType,
                bytes.length
        );
    }

    public void commitPrepared(PreparedImportFile preparedFile) {
        if (preparedFile == null) {
            return;
        }
        ensureEnabled();

        try {
            client.copyObject(CopyObjectArgs.builder()
                    .bucket(preparedFile.bucket())
                    .object(preparedFile.finalObjectKey())
                    .source(CopySource.builder()
                            .bucket(preparedFile.bucket())
                            .object(preparedFile.stagingObjectKey())
                            .build())
                    .build());
        } catch (Exception e) {
            throw new InternalServerErrorException("failed to commit import file in storage");
        }
    }

    public void rollbackPrepared(PreparedImportFile preparedFile) {
        if (preparedFile == null) {
            return;
        }
        if (!enabled) {
            return;
        }
        safeDelete(preparedFile.bucket(), preparedFile.stagingObjectKey());
        safeDelete(preparedFile.bucket(), preparedFile.finalObjectKey());
    }

    public void cleanupAfterCommit(PreparedImportFile preparedFile) {
        if (preparedFile == null) {
            return;
        }
        if (!enabled) {
            return;
        }
        safeDelete(preparedFile.bucket(), preparedFile.stagingObjectKey());
    }

    public StoredImportFile download(String objectKey, String fileName, String contentType) {
        ensureEnabled();
        if (objectKey == null || objectKey.isBlank()) {
            throw new NotFoundException("import file is not available");
        }
        try (InputStream input = client.getObject(GetObjectArgs.builder()
                .bucket(bucket)
                .object(objectKey)
                .build())) {
            return new StoredImportFile(
                    sanitizeFileName(fileName),
                    normalizeContentType(contentType),
                    input.readAllBytes()
            );
        } catch (ErrorResponseException e) {
            if ("NoSuchKey".equalsIgnoreCase(e.errorResponse().code())) {
                throw new NotFoundException("import file not found in storage");
            }
            throw new InternalServerErrorException("failed to download import file");
        } catch (Exception e) {
            throw new InternalServerErrorException("failed to download import file");
        }
    }

    private void ensureBucket() {
        try {
            boolean exists = client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                if (!autoCreateBucket) {
                    throw new IllegalStateException("MinIO bucket does not exist: " + bucket);
                }
                client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            }
        } catch (Exception e) {
            throw new IllegalStateException("failed to initialize MinIO bucket: " + bucket, e);
        }
    }

    private void safeDelete(String bucketName, String objectKey) {
        try {
            client.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectKey)
                    .build());
        } catch (Exception e) {
            LOG.log(Level.WARNING, "failed to delete object from storage: " + objectKey, e);
        }
    }

    private String sanitizeFileName(String rawName) {
        String fallback = "tickets-import.csv";
        if (rawName == null || rawName.isBlank()) {
            return fallback;
        }
        String baseName;
        try {
            baseName = Path.of(rawName).getFileName().toString();
        } catch (Exception e) {
            baseName = rawName;
        }
        String normalized = baseName.replaceAll("[^A-Za-z0-9._-]", "_");
        if (normalized.isBlank()) {
            return fallback;
        }
        return normalized;
    }

    private String normalizeContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return DEFAULT_CONTENT_TYPE;
        }
        return contentType;
    }

    private void ensureEnabled() {
        if (!enabled) {
            throw new ServiceUnavailableException("Minio не запущен");
        }
    }
}
