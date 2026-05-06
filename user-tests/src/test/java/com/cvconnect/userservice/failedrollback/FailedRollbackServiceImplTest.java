package com.cvconnect.userservice.failedrollback;

/**
 * ============================================================
 * FILE: FailedRollbackServiceImplTest.java
 * MODULE: user-service
 * PURPOSE: Unit tests for FailedRollbackServiceImpl DB delegation.
 * ============================================================
 */

import com.cvconnect.dto.failedRollback.FailedRollbackDto;
import com.cvconnect.entity.FailedRollback;
import com.cvconnect.repository.FailedRollbackRepository;
import com.cvconnect.service.impl.FailedRollbackServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("FailedRollbackServiceImpl - Unit Tests")
class FailedRollbackServiceImplTest {

    @Mock
    private FailedRollbackRepository failedRollbackRepository;

    @InjectMocks
    private FailedRollbackServiceImpl failedRollbackService;

    /**
     * Test Case ID: TC-US-FRS-001
     * Test Objective: Save failed rollback DTO to repository entity.
     * Input: FailedRollbackDto with rollback metadata.
     * Expected Output: repository.save called with mapped entity.
     * Notes: CheckDB write path.
     */
    @Test
    @DisplayName("save: map DTO and persist entity")
    void save_shouldMapAndPersistEntity() {
        FailedRollbackDto dto = FailedRollbackDto.builder()
                .type("UPLOAD_FILE")
                .payload("{\"attachFileIds\":[1,2]}")
                .errorMessage("delete failed")
                .status(false)
                .retryCount(0)
                .build();

        failedRollbackService.save(dto);

        ArgumentCaptor<FailedRollback> entityCaptor = ArgumentCaptor.forClass(FailedRollback.class);
        verify(failedRollbackRepository).save(entityCaptor.capture());
        assertThat(entityCaptor.getValue().getType()).isEqualTo("UPLOAD_FILE");
        assertThat(entityCaptor.getValue().getPayload()).contains("attachFileIds");
        assertThat(entityCaptor.getValue().getStatus()).isFalse();
        assertThat(entityCaptor.getValue().getRetryCount()).isEqualTo(0);
    }

    /**
     * Test Case ID: TC-US-FRS-002
     * Test Objective: Return empty list when repository has no pending rollbacks.
     * Input: repository.getPendingFailedRollbacks() returns empty list.
     * Expected Output: empty DTO list.
     * Notes: DB read empty branch.
     */
    @Test
    @DisplayName("getPendingFailedRollbacks: return empty list when no data")
    void getPendingFailedRollbacks_emptyResult_shouldReturnEmptyList() {
        when(failedRollbackRepository.getPendingFailedRollbacks()).thenReturn(List.of());

        List<FailedRollbackDto> result = failedRollbackService.getPendingFailedRollbacks();

        assertThat(result).isEmpty();
    }

    /**
     * Test Case ID: TC-US-FRS-003
     * Test Objective: Map repository entities to DTO list.
     * Input: repository returns one pending failed rollback entity.
     * Expected Output: mapped DTO list with matching fields.
     * Notes: DB read mapping branch.
     */
    @Test
    @DisplayName("getPendingFailedRollbacks: map entity list to DTO list")
    void getPendingFailedRollbacks_hasData_shouldMapToDtoList() {
        FailedRollback entity = new FailedRollback();
        entity.setId(99L);
        entity.setType("ORG_CREATION");
        entity.setPayload("{\"orgId\":100}");
        entity.setErrorMessage("rollback pending");
        entity.setStatus(false);
        entity.setRetryCount(1);

        when(failedRollbackRepository.getPendingFailedRollbacks()).thenReturn(List.of(entity));

        List<FailedRollbackDto> result = failedRollbackService.getPendingFailedRollbacks();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(99L);
        assertThat(result.get(0).getType()).isEqualTo("ORG_CREATION");
        assertThat(result.get(0).getRetryCount()).isEqualTo(1);
    }
}
