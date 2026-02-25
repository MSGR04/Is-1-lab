package com.msgr.tickets.service;

import com.msgr.tickets.domain.entity.AppUser;
import com.msgr.tickets.domain.entity.ImportOperation;
import com.msgr.tickets.domain.enums.ImportOperationStatus;
import com.msgr.tickets.network.dto.TicketImportHistoryDto;
import com.msgr.tickets.persistence.AppUserRepository;
import com.msgr.tickets.persistence.ImportOperationRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;

import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class ImportOperationService {

    private static final int HISTORY_LIMIT = 200;

    @Inject
    private ImportOperationRepository repo;

    @Inject
    private AppUserRepository userRepo;

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public long createStarted(long userId) {
        AppUser user = userRepo.findById(userId)
                .orElseThrow(() -> new NotFoundException("user not found: " + userId));

        ImportOperation operation = new ImportOperation();
        operation.setUser(user);
        operation.setStatus(ImportOperationStatus.IN_PROGRESS);
        operation.setImportedCount(null);
        operation.setSourceFileName(null);
        operation.setSourceFileObjectKey(null);
        operation.setSourceFileContentType(null);
        operation.setSourceFileSizeBytes(null);
        operation.setStartedAt(LocalDateTime.now());
        operation.setFinishedAt(null);

        repo.save(operation);
        return operation.getId();
    }

    @Transactional(Transactional.TxType.MANDATORY)
    public void markSuccessInCurrentTx(
            long operationId,
            int importedCount,
            String sourceFileName,
            String sourceFileObjectKey,
            String sourceFileContentType,
            long sourceFileSizeBytes
    ) {
        ImportOperation operation = repo.findById(operationId)
                .orElseThrow(() -> new NotFoundException("import operation not found: " + operationId));

        operation.setStatus(ImportOperationStatus.SUCCESS);
        operation.setImportedCount(importedCount);
        operation.setSourceFileName(sourceFileName);
        operation.setSourceFileObjectKey(sourceFileObjectKey);
        operation.setSourceFileContentType(sourceFileContentType);
        operation.setSourceFileSizeBytes(sourceFileSizeBytes);
        operation.setFinishedAt(LocalDateTime.now());
        repo.save(operation);
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void markFailed(long operationId) {
        ImportOperation operation = repo.findById(operationId)
                .orElseThrow(() -> new NotFoundException("import operation not found: " + operationId));

        operation.setStatus(ImportOperationStatus.FAILED);
        operation.setImportedCount(null);
        operation.setFinishedAt(LocalDateTime.now());
        repo.save(operation);
    }

    public ImportOperation getAllowedForDownload(long operationId, long requesterUserId, String requesterRole) {
        ImportOperation operation = repo.findById(operationId)
                .orElseThrow(() -> new NotFoundException("import operation not found: " + operationId));

        boolean isAdmin = requesterRole != null && requesterRole.equalsIgnoreCase("ADMIN");
        if (!isAdmin && !operation.getUser().getId().equals(requesterUserId)) {
            throw new ForbiddenException("no access to this import operation");
        }
        return operation;
    }

    public List<TicketImportHistoryDto> listHistory(long requesterUserId, String requesterRole) {
        boolean isAdmin = requesterRole != null && requesterRole.equalsIgnoreCase("ADMIN");
        List<ImportOperation> operations = isAdmin
                ? repo.findRecentAll(HISTORY_LIMIT)
                : repo.findRecentByUser(requesterUserId, HISTORY_LIMIT);

        return operations.stream()
                .map(this::toDto)
                .toList();
    }

    private TicketImportHistoryDto toDto(ImportOperation operation) {
        Integer importedCount = operation.getStatus() == ImportOperationStatus.SUCCESS
                ? operation.getImportedCount()
                : null;

        return new TicketImportHistoryDto(
                operation.getId(),
                operation.getStatus().name(),
                operation.getUser().getUsername(),
                importedCount,
                operation.getSourceFileName(),
                operation.getSourceFileObjectKey() != null && !operation.getSourceFileObjectKey().isBlank()
        );
    }
}
