package com.cvconnect.service.impl;

/**
 * ============================================================
 * FILE: JobAdProcessServiceImplTest.java
 * MODULE: core-service
 * PURPOSE: Unit test cho hệ thống JobAdProcess Service
 *
 * BAO PHỦ CÁC LUỒNG CẤP 2 (Branch Coverage):
 *   - create: list rỗng vs list có dữ liệu.
 *   - getByJobAdId: nhánh List empty vs tồn tại map data từ processTypeService.
 *   - getById: nhánh Null Optional vs Loop Matching processTypeId.
 *   - getJobAdProcessByJobAdIds: Error fallback, NumberOfApplicants null handler = 0L.
 * ============================================================
 */

import com.cvconnect.dto.jobAd.JobAdProcessDto;
import com.cvconnect.dto.jobAdCandidate.JobAdProcessProjection;
import com.cvconnect.dto.processType.ProcessTypeDto;
import com.cvconnect.entity.JobAdProcess;
import com.cvconnect.repository.JobAdProcessRepository;
import com.cvconnect.service.ProcessTypeService;
import com.cvconnect.service.impl.JobAdProcessServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JobAdProcessServiceImpl - Unit Tests (C2 Branch Coverage)")
public class JobAdProcessServiceImplTest {

    @Mock
    private JobAdProcessRepository jobAdProcessRepository;

    @Mock
    private ProcessTypeService processTypeService;

    @InjectMocks
    private JobAdProcessServiceImpl jobAdProcessService;

    /**
     * Test Case ID: TC-JAP-001
     * Test Objective: Validate TC_JAP_001_createEmpty behavior.
     * Input: Dữ liệu mock theo ngữ cảnh test case.
     * Expected Output: Kết quả đúng theo assert/verify của test case.
     * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
     */
    @Test
    @DisplayName("TC-JAP-001: Không save khi rỗng")
    void TC_JAP_001_createEmpty() {
        jobAdProcessService.create(Collections.emptyList());
        verify(jobAdProcessRepository, never()).saveAll(any());
    }

    /**
     * Test Case ID: TC-JAP-002
     * Test Objective: Validate TC_JAP_002_createData behavior.
     * Input: Dữ liệu mock theo ngữ cảnh test case.
     * Expected Output: Kết quả đúng theo assert/verify của test case.
     * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
     */
    @Test
    @DisplayName("TC-JAP-002: SaveAll khi có data")
    void TC_JAP_002_createData() {
        JobAdProcessDto dto = new JobAdProcessDto(); dto.setId(10L);
        jobAdProcessService.create(List.of(dto));
        verify(jobAdProcessRepository).saveAll(any());
    }

    /**
     * Test Case ID: TC-JAP-003
     * Test Objective: Validate TC_JAP_003_getByJobAdIdEmpty behavior.
     * Input: Dữ liệu mock theo ngữ cảnh test case.
     * Expected Output: Kết quả đúng theo assert/verify của test case.
     * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
     */
    @Test
    @DisplayName("TC-JAP-003: Lấy danh sách rỗng từ JobAdId")
    void TC_JAP_003_getByJobAdIdEmpty() {
        when(jobAdProcessRepository.findByJobAdId(99L)).thenReturn(Collections.emptyList());
        List<JobAdProcessDto> res = jobAdProcessService.getByJobAdId(99L);
        assertThat(res).isEmpty();
    }

    /**
     * Test Case ID: TC-JAP-004
     * Test Objective: Validate TC_JAP_004_getByJobAdIdSuccess behavior.
     * Input: Dữ liệu mock theo ngữ cảnh test case.
     * Expected Output: Kết quả đúng theo assert/verify của test case.
     * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
     */
    @Test
    @DisplayName("TC-JAP-004: Lấy danh sách thành công và map Process Type")
    void TC_JAP_004_getByJobAdIdSuccess() {
        JobAdProcess p1 = new JobAdProcess(); p1.setProcessTypeId(5L);
        when(jobAdProcessRepository.findByJobAdId(99L)).thenReturn(List.of(p1));

        ProcessTypeDto tDto = new ProcessTypeDto(); tDto.setId(5L); tDto.setName("Type 5");
        when(processTypeService.getAllProcessType()).thenReturn(List.of(tDto));

        List<JobAdProcessDto> res = jobAdProcessService.getByJobAdId(99L);
        assertThat(res).hasSize(1);
        assertThat(res.get(0).getProcessType().getName()).isEqualTo("Type 5");
    }

    /**
     * Test Case ID: TC-JAP-005
     * Test Objective: Validate TC_JAP_005_getByIdEmpty behavior.
     * Input: Dữ liệu mock theo ngữ cảnh test case.
     * Expected Output: Kết quả đúng theo assert/verify của test case.
     * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
     */
    @Test
    @DisplayName("TC-JAP-005: getById trả về null khi Optional empty")
    void TC_JAP_005_getByIdEmpty() {
        when(jobAdProcessRepository.findById(1L)).thenReturn(Optional.empty());
        JobAdProcessDto res = jobAdProcessService.getById(1L);
        assertThat(res).isNull();
    }

    /**
     * Test Case ID: TC-JAP-006
     * Test Objective: Validate TC_JAP_006_getByIdSuccessMatches behavior.
     * Input: Dữ liệu mock theo ngữ cảnh test case.
     * Expected Output: Kết quả đúng theo assert/verify của test case.
     * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
     */
    @Test
    @DisplayName("TC-JAP-006: getById parse Entity và loop map matching ProcessTypeId")
    void TC_JAP_006_getByIdSuccessMatches() {
        JobAdProcess entity = new JobAdProcess(); entity.setId(10L); entity.setProcessTypeId(7L);
        when(jobAdProcessRepository.findById(10L)).thenReturn(Optional.of(entity));

        ProcessTypeDto t1 = new ProcessTypeDto(); t1.setId(1L);
        ProcessTypeDto t2 = new ProcessTypeDto(); t2.setId(7L); t2.setName("Matched Name");
        when(processTypeService.getAllProcessType()).thenReturn(List.of(t1, t2)); // Loop testing

        JobAdProcessDto res = jobAdProcessService.getById(10L);
        assertThat(res.getProcessType().getName()).isEqualTo("Matched Name");
    }

    /**
     * Test Case ID: TC-JAP-007
     * Test Objective: Validate TC_JAP_007_existMethod behavior.
     * Input: Dữ liệu mock theo ngữ cảnh test case.
     * Expected Output: Kết quả đúng theo assert/verify của test case.
     * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
     */
    @Test
    @DisplayName("TC-JAP-007: exist boolean bypass")
    void TC_JAP_007_existMethod() {
        when(jobAdProcessRepository.existByJobAdProcessIdAndOrgId(1L, 2L)).thenReturn(true);
        assertThat(jobAdProcessService.existByJobAdProcessIdAndOrgId(1L, 2L)).isTrue();
    }

    /**
     * Test Case ID: TC-JAP-008
     * Test Objective: Validate TC_JAP_008_groupingEmpty behavior.
     * Input: Dữ liệu mock theo ngữ cảnh test case.
     * Expected Output: Kết quả đúng theo assert/verify của test case.
     * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
     */
    @Test
    @DisplayName("TC-JAP-008: Grouping projections null fallback empty Map")
    void TC_JAP_008_groupingEmpty() {
        when(jobAdProcessRepository.findJobAdProcessByJobAdIds(any())).thenReturn(Collections.emptyList());
        Map<Long, List<JobAdProcessDto>> res = jobAdProcessService.getJobAdProcessByJobAdIds(List.of(1L));
        assertThat(res).isEmpty();
    }

    /**
     * Test Case ID: TC-JAP-009
     * Test Objective: Validate TC_JAP_009_groupingHandleNullNum behavior.
     * Input: Dữ liệu mock theo ngữ cảnh test case.
     * Expected Output: Kết quả đúng theo assert/verify của test case.
     * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
     */
    @Test
    @DisplayName("TC-JAP-009: Grouping projections mapping null value numberOfApplicants to 0L")
    void TC_JAP_009_groupingHandleNullNum() {
        JobAdProcessProjection proj1 = Mockito.mock(JobAdProcessProjection.class);
        when(proj1.getJobAdId()).thenReturn(50L);
        when(proj1.getNumberOfApplicants()).thenReturn(null); // Null handle check

        JobAdProcessProjection proj2 = Mockito.mock(JobAdProcessProjection.class);
        when(proj2.getJobAdId()).thenReturn(50L);
        when(proj2.getNumberOfApplicants()).thenReturn(9L);

        when(jobAdProcessRepository.findJobAdProcessByJobAdIds(any())).thenReturn(List.of(proj1, proj2));

        Map<Long, List<JobAdProcessDto>> res = jobAdProcessService.getJobAdProcessByJobAdIds(List.of(50L));
        
        List<JobAdProcessDto> group50 = res.get(50L);
        assertThat(group50).hasSize(2);
        assertThat(group50.get(0).getNumberOfApplicants()).isEqualTo(0L); // C2 branch
        assertThat(group50.get(1).getNumberOfApplicants()).isEqualTo(9L);
    }
}


