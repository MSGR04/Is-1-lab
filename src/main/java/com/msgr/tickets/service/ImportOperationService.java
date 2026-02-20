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
        operation.setStartedAt(LocalDateTime.now());
        operation.setFinishedAt(null);

        repo.save(operation);
        return operation.getId();
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void markSuccess(long operationId, int importedCount) {
        ImportOperation operation = repo.findById(operationId)
                .orElseThrow(() -> new NotFoundException("import operation not found: " + operationId));

        operation.setStatus(ImportOperationStatus.SUCCESS);
        operation.setImportedCount(importedCount);
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
                importedCount
        );
    }
}
