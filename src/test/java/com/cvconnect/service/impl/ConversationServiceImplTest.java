package com.cvconnect.service.impl;

import com.cvconnect.collection.Conversation;
import com.cvconnect.common.RestTemplateClient;
import com.cvconnect.config.socket.SocketHandler;
import com.cvconnect.dto.ConversationRequest;
import com.cvconnect.enums.NotifyErrorCode;
import com.cvconnect.repository.ConversationRepository;
import nmquan.commonlib.dto.response.IDResponse;
import nmquan.commonlib.exception.AppException;
import nmquan.commonlib.utils.WebUtils;
import com.cvconnect.service.MongoQueryService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ConversationServiceImplTest {

    @Mock
    private ConversationRepository conversationRepository;
    @Mock
    private RestTemplateClient restTemplateClient;
    @Mock
    private SocketHandler socketHandler;

    @Mock
    private MongoQueryService mongoQueryService;

    @InjectMocks
    private ConversationServiceImpl conversationService;

    private Long mockUserId = 100L;
    private Long mockCandId = 200L;
    private Long mockHrId = 100L;

    @Test
    @DisplayName("TC-NTF-CV-001: Create Conversation Success")
    void create_Success() {
        try (MockedStatic<WebUtils> webUtils = mockStatic(WebUtils.class)) {
            webUtils.when(WebUtils::getCurrentUserId).thenReturn(mockUserId);
            
            ConversationRequest request = new ConversationRequest();
            request.setJobAdId(1L);
            request.setCandidateId(mockCandId);

            when(conversationRepository.findByJobAdIdAndCandidateId(any(), any())).thenReturn(null);
            when(restTemplateClient.validateAndGetHrContactId(anyLong(), anyLong())).thenReturn(mockHrId);

            IDResponse<String> response = conversationService.create(request);

            verify(conversationRepository).save(any(Conversation.class));
            assertThat(response).isNotNull();
        }
    }

    @Test
    @DisplayName("TC-NTF-CV-002: Create Fail - Already existed")
    void create_Fail_Existed() {
        try (MockedStatic<WebUtils> webUtils = mockStatic(WebUtils.class)) {
            webUtils.when(WebUtils::getCurrentUserId).thenReturn(mockUserId);
            
            when(conversationRepository.findByJobAdIdAndCandidateId(any(), any())).thenReturn(new Conversation());

            ConversationRequest request = new ConversationRequest();
            request.setJobAdId(1L);
            request.setCandidateId(mockCandId);

            assertThatThrownBy(() -> conversationService.create(request))
                    .isInstanceOf(AppException.class);
        }
    }

    @Test
    @DisplayName("TC-NTF-CV-008: NewMessage Success")
    void newMessage_Success() {
        try (MockedStatic<WebUtils> webUtils = mockStatic(WebUtils.class)) {
            webUtils.when(WebUtils::getCurrentUserId).thenReturn(mockUserId);
            
            Conversation conversation = new Conversation();
            conversation.setParticipantIds(List.of(mockUserId, mockCandId));

            when(conversationRepository.findByJobAdIdAndCandidateId(any(), any())).thenReturn(conversation);
            when(restTemplateClient.getJobAdCandidateData(anyLong(), anyLong())).thenReturn(new com.cvconnect.dto.DataJobAdCandidate());

            com.cvconnect.dto.ChatMessageRequest request = new com.cvconnect.dto.ChatMessageRequest();
            request.setJobAdId(1L);
            request.setCandidateId(mockCandId);
            request.setText("Hello");

            IDResponse<String> response = conversationService.newMessage(request, mockUserId);

            assertThat(response).isNotNull();
            verify(conversationRepository).save(any(Conversation.class));
        }
    }

    @Test
    @DisplayName("TC-NTF-CV-012: ReadAll Success")
    void readAllMessages_Success() {
        try (MockedStatic<WebUtils> webUtils = mockStatic(WebUtils.class)) {
            webUtils.when(WebUtils::getCurrentUserId).thenReturn(mockUserId);
            
            Conversation conversation = new Conversation();
            conversation.setId("conv1");
            conversation.setParticipantIds(List.of(mockUserId, mockCandId));
            conversation.setMessages(List.of(new com.cvconnect.collection.ChatMessage()));

            when(conversationRepository.findByJobAdIdAndCandidateId(any(), any())).thenReturn(conversation);

            ConversationRequest request = new ConversationRequest();
            request.setJobAdId(1L);
            request.setCandidateId(mockCandId);

            conversationService.readAllMessages(request);

            verify(conversationRepository).markAllMessagesAsRead(eq("conv1"), eq(mockUserId));
        }
    }

    @Test
    @DisplayName("TC-NTF-CV-015: checkUnread Candidate Success")
    void checkUnread_Candidate() {
        try (MockedStatic<WebUtils> webUtils = mockStatic(WebUtils.class)) {
            webUtils.when(WebUtils::getCurrentUserId).thenReturn(mockUserId);
            when(conversationRepository.findAnyUnreadMessageCandidate(anyLong())).thenReturn(List.of(new Conversation()));

            Boolean result = conversationService.checkExistsConversationWithUnreadMessages(true);

            assertThat(result).isTrue();
        }
    }

    @Test
    @DisplayName("TC-NTF-CV-022: getChatMessages Success")
    void getChatMessages_Success() {
        try (MockedStatic<WebUtils> webUtils = mockStatic(WebUtils.class)) {
            webUtils.when(WebUtils::getCurrentUserId).thenReturn(mockUserId);
            
            com.cvconnect.dto.ConversationDto mockResp = new com.cvconnect.dto.ConversationDto();
            mockResp.setId("conv1");
            mockResp.setParticipantIds(List.of(mockUserId, mockCandId));
            
            when(mongoQueryService.getChatMessages(any())).thenReturn(mockResp);
            when(conversationRepository.findById(anyString())).thenReturn(java.util.Optional.of(new Conversation()));

            com.cvconnect.dto.ChatMessageFilter filter = new com.cvconnect.dto.ChatMessageFilter();
            com.cvconnect.dto.ConversationDto result = conversationService.getChatMessages(filter);

            assertThat(result).isNotNull();
        }
    }

    @ParameterizedTest
    @DisplayName("TC-NTF-CV-031-045: NewMessage Validation with various texts")
    @ValueSource(strings = {"Hello", "Hi", "Good morning", "Test message", "How are you?", "See you", "OK", "Thanks", "Interested", "Apply", "Interview", "Offer", "Reject", "Wait", "Urgent"})
    void newMessage_Validation_Texts(String text) {
        try (MockedStatic<WebUtils> webUtils = mockStatic(WebUtils.class)) {
            webUtils.when(WebUtils::getCurrentUserId).thenReturn(mockUserId);
            Conversation conversation = new Conversation();
            conversation.setParticipantIds(List.of(mockUserId, mockCandId));
            when(conversationRepository.findByJobAdIdAndCandidateId(any(), any())).thenReturn(conversation);
            when(restTemplateClient.getJobAdCandidateData(anyLong(), anyLong())).thenReturn(new com.cvconnect.dto.DataJobAdCandidate());

            com.cvconnect.dto.ChatMessageRequest request = new com.cvconnect.dto.ChatMessageRequest();
            request.setJobAdId(1L);
            request.setCandidateId(mockCandId);
            request.setText(text);
            conversationService.newMessage(request, mockUserId);
            verify(conversationRepository).save(any(Conversation.class));
        }
    }

    @ParameterizedTest
    @DisplayName("TC-NTF-CV-046-060: checkUnread Logic for multiple users")
    @CsvSource({
        "100, true", "101, true", "102, true", "103, true", "104, true",
        "200, false", "201, false", "202, false", "203, false", "204, false",
        "300, true", "301, true", "302, true", "303, true", "304, true"
    })
    void checkUnread_Parameterized(Long userId, Boolean isCandidate) {
        try (MockedStatic<WebUtils> webUtils = mockStatic(WebUtils.class)) {
            webUtils.when(WebUtils::getCurrentUserId).thenReturn(userId);
            if(isCandidate) {
                when(conversationRepository.findAnyUnreadMessageCandidate(userId)).thenReturn(List.of(new Conversation()));
            } else {
                when(conversationRepository.findAnyUnreadMessageHr(userId)).thenReturn(List.of(new Conversation()));
            }

            Boolean result = conversationService.checkExistsConversationWithUnreadMessages(isCandidate);
            assertThat(result).isTrue();
        }
    }
}
