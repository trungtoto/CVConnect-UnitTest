package com.cvconnect.common;

/**
 * ============================================================
 * FILE: ReplacePlaceholderTest.java
 * MODULE: core-service
 * MỤC ĐÍCH: Unit test cho {@link ReplacePlaceholder}
 *
 * BAO PHỦ CÁC LUỒNG CHÍNH:
 *   - replacePlaceholder() với dữ liệu null/default/đầy đủ
 *   - previewEmail() với chế độ default và dữ liệu thực tế
 * ============================================================
 */

import com.cvconnect.dto.candidateInfoApply.CandidateInfoApplyDto;
import com.cvconnect.dto.common.DataReplacePlaceholder;
import com.cvconnect.dto.internal.response.UserDto;
import com.cvconnect.dto.jobAd.JobAdDto;
import com.cvconnect.dto.jobAd.JobAdProcessDto;
import com.cvconnect.dto.org.OrgAddressDto;
import com.cvconnect.dto.org.OrgDto;
import com.cvconnect.dto.position.PositionDto;
import com.cvconnect.service.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReplacePlaceholder - Unit Tests")
class ReplacePlaceholderTest {

    @Mock private PositionService positionService;
    @Mock private OrgService orgService;
    @Mock private JobAdService jobAdService;
    @Mock private JobAdProcessService jobAdProcessService;
    @Mock private OrgAddressService orgAddressService;
    @Mock private CandidateInfoApplyService candidateInfoApplyService;
    @Mock private RestTemplateClient restTemplateClient;

    @InjectMocks
    private ReplacePlaceholder replacePlaceholder;

    private static final String DOT_DOT_DOT = "...";
    private static final Instant START_INSTANT = Instant.parse("2024-05-20T08:30:00Z");
    private static final Instant END_INSTANT = Instant.parse("2024-05-20T09:30:00Z");

    /**
     * Test Case ID: TC-RP-001
     * Test Objective: Kiểm tra replacePlaceholder xử lý null/empty input an toàn.
     * Input: template hoặc placeholders hoặc baseData là null/rỗng.
     * Expected Output: Trả về null hoặc giữ nguyên template theo từng nhánh.
     * Notes: Bao phủ nhánh guard clause.
     */
    @Test
    @DisplayName("replacePlaceholder: Null inputs should return template")
    void replacePlaceholder_nullInputs_shouldReturnTemplate() {
        assertThat(replacePlaceholder.replacePlaceholder(null, List.of(), new DataReplacePlaceholder())).isNull();
        assertThat(replacePlaceholder.replacePlaceholder("tpl", null, new DataReplacePlaceholder())).isEqualTo("tpl");
        assertThat(replacePlaceholder.replacePlaceholder("tpl", List.of(), null)).isEqualTo("tpl");
    }

    /**
     * Test Case ID: TC-RP-002
     * Test Objective: Kiểm tra previewEmail xử lý input null.
     * Input: previewEmail với template/placeholder null.
     * Expected Output: Trả về null hoặc template gốc.
     * Notes: Bao phủ nhánh tương tự replacePlaceholder.
     */
    @Test
    @DisplayName("previewEmail: Null inputs should return template")
    void previewEmail_nullInputs_shouldReturnTemplate() {
        assertThat(replacePlaceholder.previewEmail(null, List.of(), new DataReplacePlaceholder(), true)).isNull();
        assertThat(replacePlaceholder.previewEmail("tpl", null, new DataReplacePlaceholder(), true)).isEqualTo("tpl");
    }

    /**
     * Test Case ID: TC-RP-003
     * Test Objective: Đảm bảo previewEmail trả giá trị mặc định khi isDefault=true.
     * Input: Từng placeholder mặc định từ method source defaultPlaceholders.
     * Expected Output: Mỗi placeholder được thay đúng default value.
     * Notes: Parameterized test cho toàn bộ token mặc định.
     */
    @ParameterizedTest
    @MethodSource("defaultPlaceholders")
    @DisplayName("previewEmail: isDefault=true should return all default values")
    void previewEmail_defaultValues_shouldBeReplaced(String placeholder, String expectedValue) {
        String result = replacePlaceholder.previewEmail(placeholder, List.of(placeholder), new DataReplacePlaceholder(), true);
        assertThat(result).isEqualTo(expectedValue);
    }

    private static Stream<Arguments> defaultPlaceholders() {
        return Stream.of(
            Arguments.of("${jobPosition}", "Kỹ sư phát triển phần mềm"),
            Arguments.of("${postTitle}", "Lập trình viên Java"),
            Arguments.of("${currentRound}", "Ứng tuyển"),
            Arguments.of("${nextRound}", "Phỏng vấn kỹ thuật"),
            Arguments.of("${interviewLink}", "https://meet.google.com/"),
            Arguments.of("${orgAddress}", "36A, Dich Vong Hau, Ha Noi"),
            Arguments.of("${candidateName}", "Nguyễn Minh Quân"),
            Arguments.of("${orgName}", "Công ty Cổ phần ABC"),
            Arguments.of("${hrName}", "Nguyễn Văn A"),
            Arguments.of("${hrPhone}", "0901234567"),
            Arguments.of("${hrEmail}", "test@gmail.com"),
            Arguments.of("${examDate}", "01/12/2025"),
            Arguments.of("${startTime}", "08:00"),
            Arguments.of("${endTime}", "10:00"),
            Arguments.of("${examDuration}", "120"),
            Arguments.of("${interview-examLocation}", "36A, Dich Vong Hau, Ha Noi")
        );
    }

    /**
     * Test Case ID: TC-RP-004
     * Test Objective: Kiểm tra fallback khi baseData=null và isDefault=false.
     * Input: Placeholder từ method source previewWithNullBaseData.
     * Expected Output: Trả về DOT_DOT_DOT hoặc giá trị mặc định đặc thù (salutation).
     * Notes: Bao phủ nhánh fallback không dùng default template set.
     */
    @ParameterizedTest
    @MethodSource("previewWithNullBaseData")
    @DisplayName("previewEmail: baseData=null and isDefault=false should use fallback values")
    void previewEmail_baseDataNull_shouldFallback(String placeholder, String expectedValue) {
        String result = replacePlaceholder.previewEmail(placeholder, List.of(placeholder), null, false);
        assertThat(result).isEqualTo(expectedValue);
    }

    private static Stream<Arguments> previewWithNullBaseData() {
        return Stream.of(
            Arguments.of("${jobPosition}", DOT_DOT_DOT),
            Arguments.of("${postTitle}", DOT_DOT_DOT),
            Arguments.of("${currentRound}", DOT_DOT_DOT),
            Arguments.of("${nextRound}", DOT_DOT_DOT),
            Arguments.of("${interviewLink}", DOT_DOT_DOT),
            Arguments.of("${orgAddress}", DOT_DOT_DOT),
            Arguments.of("${candidateName}", DOT_DOT_DOT),
            Arguments.of("${salutation}", "Anh/Chị"),
            Arguments.of("${orgName}", DOT_DOT_DOT),
            Arguments.of("${hrName}", DOT_DOT_DOT),
            Arguments.of("${hrPhone}", DOT_DOT_DOT),
            Arguments.of("${hrEmail}", DOT_DOT_DOT),
            Arguments.of("${examDate}", DOT_DOT_DOT),
            Arguments.of("${startTime}", DOT_DOT_DOT),
            Arguments.of("${endTime}", DOT_DOT_DOT),
            Arguments.of("${examDuration}", DOT_DOT_DOT),
            Arguments.of("${interview-examLocation}", DOT_DOT_DOT)
        );
    }

    /**
     * Test Case ID: TC-RP-005
     * Test Objective: Ưu tiên dùng giá trị trực tiếp từ baseData khi có đủ dữ liệu.
     * Input: DataReplacePlaceholder được set đầy đủ các trường trực tiếp.
     * Expected Output: Template được thay đầy đủ đúng giá trị và format ngày giờ.
     * Notes: Không phụ thuộc service lookup.
     */
    @Test
    @DisplayName("replacePlaceholder: should prefer direct values from baseData")
    void replacePlaceholder_shouldPreferDirectValues() {
        DataReplacePlaceholder data = new DataReplacePlaceholder();
        data.setPositionName("Senior Java");
        data.setJobAdName("JD Java 01");
        data.setJobAdProcessName("Step 1");
        data.setNextJobAdProcessName("Step 2");
        data.setInterviewLink("link.com");
        data.setOrgAddress("Hanoi Office");
        data.setCandidateName("Quan NM");
        data.setOrgName("CVConnect");
        data.setHrName("HR Name");
        data.setHrPhone("01234");
        data.setHrEmail("hr@g.com");
        data.setExamDuration(60);
        data.setLocationName("Meeting Room A");

        data.setExamStartTime(START_INSTANT);
        data.setExamEndTime(END_INSTANT);

        String template = "Position: ${jobPosition}, Job: ${postTitle}, Current: ${currentRound}, Next: ${nextRound}, Link: ${interviewLink}, " +
                "Addr: ${orgAddress}, Candidate: ${candidateName}, Salutation: ${salutation}, Org: ${orgName}, " +
                "HR: ${hrName}, Phone: ${hrPhone}, Email: ${hrEmail}, " +
                "Date: ${examDate}, Start: ${startTime}, End: ${endTime}, Duration: ${examDuration}, Loc: ${interview-examLocation}";
        
        List<String> tokens = List.of("${jobPosition}", "${postTitle}", "${currentRound}", "${nextRound}", "${interviewLink}",
                "${orgAddress}", "${candidateName}", "${salutation}", "${orgName}", "${hrName}", "${hrPhone}", "${hrEmail}",
                "${examDate}", "${startTime}", "${endTime}", "${examDuration}", "${interview-examLocation}");
        
        String result = replacePlaceholder.replacePlaceholder(template, tokens, data);
        
        assertThat(result).contains("Position: Senior Java")
                .contains("Job: JD Java 01")
                .contains("Current: Step 1")
                .contains("Next: Step 2")
                .contains("Link: link.com")
                .contains("Addr: Hanoi Office")
                .contains("Candidate: Quan NM")
                .contains("Salutation: Anh/Chị")
                .contains("Org: CVConnect")
                .contains("HR: HR Name")
                .contains("Phone: 01234")
                .contains("Email: hr@g.com")
                .contains("Date: 20/05/2024")
                .contains("Start: 15:30")
                .contains("End: 16:30")
                .contains("Duration: 60")
                .contains("Loc: Meeting Room A");
    }

    /**
     * Test Case ID: TC-RP-006
     * Test Objective: Dùng dữ liệu từ service khi chỉ có ID và thiếu giá trị trực tiếp.
     * Input: baseData chứa các ID; mock service trả dữ liệu tương ứng.
     * Expected Output: Kết quả thay thế lấy từ dữ liệu service.
     * Notes: Bao phủ flow fetch-by-id.
     */
    @Test
    @DisplayName("replacePlaceholder: should use services when IDs exist and direct values are missing")
    void replacePlaceholder_shouldUseServicesWhenOnlyIdsPresent() {
        DataReplacePlaceholder data = new DataReplacePlaceholder();
        data.setPositionId(1L);
        data.setJobAdId(2L);
        data.setJobAdProcessId(3L);
        data.setNextJobAdProcessId(4L);
        data.setOrgAddressId(5L);
        data.setCandidateInfoApplyId(10L);
        data.setOrgId(20L);
        data.setHrContactId(30L);
        data.setLocationId(40L);

        lenient().when(positionService.findById(1L)).thenReturn(PositionDto.builder().name("Pos").build());
        lenient().when(jobAdService.findById(2L)).thenReturn(JobAdDto.builder().title("Title").build());
        lenient().when(jobAdProcessService.getById(3L)).thenReturn(JobAdProcessDto.builder().name("P1").build());
        lenient().when(jobAdProcessService.getById(4L)).thenReturn(JobAdProcessDto.builder().name("P2").build());
        lenient().when(orgAddressService.getById(5L)).thenReturn(OrgAddressDto.builder().displayAddress("Addr").build());
        lenient().when(candidateInfoApplyService.getById(10L)).thenReturn(CandidateInfoApplyDto.builder().fullName("Cand").build());
        lenient().when(orgService.findById(20L)).thenReturn(OrgDto.builder().name("Org").build());

        UserDto hr = new UserDto(); hr.setFullName("HR"); hr.setPhoneNumber("123"); hr.setEmail("hr@g");
        lenient().when(restTemplateClient.getUser(30L)).thenReturn(hr);

        OrgAddressDto loc = OrgAddressDto.builder().displayAddress("Loc").build();
        lenient().when(orgAddressService.getById(40L)).thenReturn(loc);
        lenient().when(orgAddressService.buildDisplayAddress(loc)).thenReturn("Loc Built");

        String template = "${jobPosition}|${postTitle}|${currentRound}|${nextRound}|${orgAddress}|${candidateName}|${orgName}|${hrName}|${hrPhone}|${hrEmail}|${interview-examLocation}";
        List<String> tokens = List.of("${jobPosition}", "${postTitle}", "${currentRound}", "${nextRound}", "${orgAddress}", "${candidateName}", "${orgName}", "${hrName}", "${hrPhone}", "${hrEmail}", "${interview-examLocation}");
        
        String result = replacePlaceholder.replacePlaceholder(template, tokens, data);
        assertThat(result).isEqualTo("Pos|Title|P1|P2|Addr|Cand|Org|HR|123|hr@g|Loc Built");
    }

    /**
     * Test Case ID: TC-RP-007
     * Test Objective: Fallback khi ID bị thiếu cho từng placeholder dạng ID-based.
     * Input: Từng placeholder với ID null từ method source idOnlyFallbackCases.
     * Expected Output: Trả về DOT_DOT_DOT.
     * Notes: Parameterized theo từng loại token.
     */
    @ParameterizedTest
    @MethodSource("idOnlyFallbackCases")
    @DisplayName("replacePlaceholder: ID missing should fallback to ...")
    void replacePlaceholder_idMissing_shouldFallbackToDots(String placeholder, java.util.function.Consumer<DataReplacePlaceholder> idSetter) {
        DataReplacePlaceholder data = new DataReplacePlaceholder();
        idSetter.accept(data);

        String result = replacePlaceholder.replacePlaceholder(placeholder, List.of(placeholder), data);
        assertThat(result).isEqualTo(DOT_DOT_DOT);
    }

    private static Stream<Arguments> idOnlyFallbackCases() {
        return Stream.of(
            Arguments.of("${jobPosition}", (java.util.function.Consumer<DataReplacePlaceholder>) d -> d.setPositionId(null)),
            Arguments.of("${postTitle}", (java.util.function.Consumer<DataReplacePlaceholder>) d -> d.setJobAdId(null)),
            Arguments.of("${currentRound}", (java.util.function.Consumer<DataReplacePlaceholder>) d -> d.setJobAdProcessId(null)),
            Arguments.of("${nextRound}", (java.util.function.Consumer<DataReplacePlaceholder>) d -> d.setNextJobAdProcessId(null)),
            Arguments.of("${orgAddress}", (java.util.function.Consumer<DataReplacePlaceholder>) d -> d.setOrgAddressId(null)),
            Arguments.of("${candidateName}", (java.util.function.Consumer<DataReplacePlaceholder>) d -> d.setCandidateInfoApplyId(null)),
            Arguments.of("${orgName}", (java.util.function.Consumer<DataReplacePlaceholder>) d -> d.setOrgId(null)),
            Arguments.of("${hrName}", (java.util.function.Consumer<DataReplacePlaceholder>) d -> d.setHrContactId(null)),
            Arguments.of("${hrPhone}", (java.util.function.Consumer<DataReplacePlaceholder>) d -> d.setHrContactId(null)),
            Arguments.of("${hrEmail}", (java.util.function.Consumer<DataReplacePlaceholder>) d -> d.setHrContactId(null)),
            Arguments.of("${interview-examLocation}", (java.util.function.Consumer<DataReplacePlaceholder>) d -> d.setLocationId(null))
        );
    }

    /**
     * Test Case ID: TC-RP-008
     * Test Objective: Fallback khi service trả null/thiếu dữ liệu.
     * Input: baseData có ID hợp lệ nhưng mock service trả object rỗng/null.
     * Expected Output: Trả về DOT_DOT_DOT hoặc giữ token gốc theo rule.
     * Notes: Bao phủ nhánh degrade gracefully.
     */
    @ParameterizedTest
    @MethodSource("serviceNullOrMissingCases")
    @DisplayName("replacePlaceholder: service returns null/missing should fallback to ...")
    void replacePlaceholder_serviceReturnsMissing_shouldFallback(String placeholder, DataReplacePlaceholder data, String expected) {
        lenient().when(positionService.findById(1L)).thenReturn(new PositionDto());
        lenient().when(jobAdService.findById(1L)).thenReturn(new JobAdDto());
        lenient().when(jobAdProcessService.getById(1L)).thenReturn(new JobAdProcessDto());
        lenient().when(orgAddressService.getById(1L)).thenReturn(new OrgAddressDto());
        lenient().when(candidateInfoApplyService.getById(1L)).thenReturn(new CandidateInfoApplyDto());
        lenient().when(orgService.findById(1L)).thenReturn(new OrgDto());
        lenient().when(restTemplateClient.getUser(1L)).thenReturn(new UserDto());
        lenient().when(orgAddressService.buildDisplayAddress(org.mockito.ArgumentMatchers.any())).thenReturn(null);

        String result = replacePlaceholder.replacePlaceholder(placeholder, List.of(placeholder), data);
        assertThat(result).isEqualTo(expected);
    }

    private static Stream<Arguments> serviceNullOrMissingCases() {
        DataReplacePlaceholder data = new DataReplacePlaceholder();
        data.setPositionId(1L);
        data.setJobAdId(1L);
        data.setJobAdProcessId(1L);
        data.setNextJobAdProcessId(1L);
        data.setOrgAddressId(1L);
        data.setCandidateInfoApplyId(1L);
        data.setOrgId(1L);
        data.setHrContactId(1L);
        data.setLocationId(1L);

        return Stream.of(
            Arguments.of("${jobPosition}", data, DOT_DOT_DOT),
            Arguments.of("${postTitle}", data, DOT_DOT_DOT),
            Arguments.of("${currentRound}", data, DOT_DOT_DOT),
            Arguments.of("${nextRound}", data, DOT_DOT_DOT),
            Arguments.of("${orgAddress}", data, DOT_DOT_DOT),
            Arguments.of("${candidateName}", data, DOT_DOT_DOT),
            Arguments.of("${orgName}", data, DOT_DOT_DOT),
            Arguments.of("${hrName}", data, DOT_DOT_DOT),
            Arguments.of("${hrPhone}", data, DOT_DOT_DOT),
            Arguments.of("${hrEmail}", data, DOT_DOT_DOT),
            Arguments.of("${interview-examLocation}", data, "${interview-examLocation}")
        );
    }

    /**
     * Test Case ID: TC-RP-009
     * Test Objective: Bao phủ nhánh xử lý placeholder ngày/giờ khi null và khi có dữ liệu.
     * Input: baseData trước và sau khi set examStartTime/examEndTime.
     * Expected Output: Null branch trả DOT_DOT_DOT, data branch trả chuỗi đã format.
     * Notes: Kiểm tra format timezone/locale hiện tại của util.
     */
    @Test
    @DisplayName("replacePlaceholder: date/time placeholders should handle null and format paths")
    void replacePlaceholder_dateTime_shouldHandleBothBranches() {
        DataReplacePlaceholder data = new DataReplacePlaceholder();

        assertThat(replacePlaceholder.replacePlaceholder("${examDate}", List.of("${examDate}"), data)).isEqualTo(DOT_DOT_DOT);
        assertThat(replacePlaceholder.replacePlaceholder("${startTime}", List.of("${startTime}"), data)).isEqualTo(DOT_DOT_DOT);
        assertThat(replacePlaceholder.replacePlaceholder("${endTime}", List.of("${endTime}"), data)).isEqualTo(DOT_DOT_DOT);

        data.setExamStartTime(START_INSTANT);
        data.setExamEndTime(END_INSTANT);

        assertThat(replacePlaceholder.replacePlaceholder("${examDate}", List.of("${examDate}"), data)).isEqualTo("20/05/2024");
        assertThat(replacePlaceholder.replacePlaceholder("${startTime}", List.of("${startTime}"), data)).isEqualTo("15:30");
        assertThat(replacePlaceholder.replacePlaceholder("${endTime}", List.of("${endTime}"), data)).isEqualTo("16:30");
    }

    /**
     * Test Case ID: TC-RP-010
     * Test Objective: Bao phủ nhánh examDuration và interviewLink cho null/data.
     * Input: baseData ban đầu null field, sau đó set duration/link.
     * Expected Output: Null -> DOT_DOT_DOT, có data -> trả đúng giá trị.
     * Notes: Trường đơn giản không cần service.
     */
    @Test
    @DisplayName("replacePlaceholder: examDuration/interviewLink should handle null and data branches")
    void replacePlaceholder_simpleFields_shouldHandleBothBranches() {
        DataReplacePlaceholder data = new DataReplacePlaceholder();

        assertThat(replacePlaceholder.replacePlaceholder("${examDuration}", List.of("${examDuration}"), data)).isEqualTo(DOT_DOT_DOT);
        assertThat(replacePlaceholder.replacePlaceholder("${interviewLink}", List.of("${interviewLink}"), data)).isEqualTo(DOT_DOT_DOT);

        data.setExamDuration(90);
        data.setInterviewLink("https://meet.google.com/abc");

        assertThat(replacePlaceholder.replacePlaceholder("${examDuration}", List.of("${examDuration}"), data)).isEqualTo("90");
        assertThat(replacePlaceholder.replacePlaceholder("${interviewLink}", List.of("${interviewLink}"), data)).isEqualTo("https://meet.google.com/abc");
    }

    /**
     * Test Case ID: TC-RP-011
     * Test Objective: Đảm bảo salutation luôn trả giá trị cố định.
     * Input: Gọi replacePlaceholder và previewEmail với token salutation.
     * Expected Output: Luôn trả "Anh/Chị".
     * Notes: Rule nghiệp vụ đặc thù, không phụ thuộc dữ liệu đầu vào.
     */
    @Test
    @DisplayName("replacePlaceholder: salutation should always return Anh/Chị")
    void replacePlaceholder_salutation_shouldAlwaysReturnFixedValue() {
        assertThat(replacePlaceholder.replacePlaceholder("${salutation}", List.of("${salutation}"), new DataReplacePlaceholder()))
            .isEqualTo("Anh/Chị");
        assertThat(replacePlaceholder.previewEmail("${salutation}", List.of("${salutation}"), null, false))
            .isEqualTo("Anh/Chị");
    }

    /**
     * Test Case ID: TC-RP-012
     * Test Objective: Đảm bảo token không xác định không bị thay sai.
     * Input: Placeholder `${unknown}`.
     * Expected Output: Giữ nguyên token `${unknown}`.
     * Notes: Bảo toàn template cho placeholder không hỗ trợ.
     */
    @Test
    @DisplayName("replacePlaceholder: unknown placeholder should keep original token")
    void replacePlaceholder_unknownPlaceholder_shouldKeepOriginalText() {
        String result = replacePlaceholder.replacePlaceholder("${unknown}", List.of("${unknown}"), new DataReplacePlaceholder());
        assertThat(result).isEqualTo("${unknown}");
    }
}
