package com.cvconnect.service.impl;

import com.cvconnect.collection.Notification;
import com.cvconnect.dto.ChatMessageFilter;
import com.cvconnect.dto.ConversationDto;
import com.cvconnect.dto.NotificationFilterRequest;
import com.cvconnect.dto.internal.request.MyConversationWithFilter;
import nmquan.commonlib.dto.response.FilterResponse;
import org.bson.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Query;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class MongoQueryServiceImplTest {

    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private MongoQueryServiceImpl mongoQueryService;

    @Test
    @DisplayName("TC-NTF-MQ-001: findNotificationWithFilter - Full Filter")
    void findNotificationWithFilter_Full() {
        NotificationFilterRequest request = new NotificationFilterRequest();
        request.setReceiverId(1L);
        request.setIsRead(false);
        request.setType("JOB");
        request.setCreatedAtStart(Instant.now());
        request.setCreatedAtEnd(Instant.now());

        when(mongoTemplate.count(any(Query.class), eq(Notification.class))).thenReturn(10L);
        when(mongoTemplate.find(any(Query.class), eq(Notification.class))).thenReturn(List.of(new Notification()));

        Page<Notification> result = mongoQueryService.findNotificationWithFilter(request, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(10L);
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("TC-NTF-MQ-005: getMyConversationsWithFilter - Unread Only")
    void getMyConversationsWithFilter_Unread() {
        MyConversationWithFilter filter = new MyConversationWithFilter();
        filter.setUserId(1L);
        filter.setHasMessagesUnread(true);

        AggregationResults<ConversationDto> mockResults = new AggregationResults<>(List.of(new ConversationDto()), new Document());
        when(mongoTemplate.aggregate(any(Aggregation.class), eq("conversations"), eq(ConversationDto.class)))
                .thenReturn(mockResults);

        AggregationResults<Document> mockCount = new AggregationResults<>(List.of(new Document("total", 5)), new Document());
        when(mongoTemplate.aggregate(any(Aggregation.class), eq("conversations"), eq(Document.class)))
                .thenReturn(mockCount);

        FilterResponse<ConversationDto> response = mongoQueryService.getMyConversationsWithFilter(filter, Instant.now(), 10);

        assertThat(response.getData()).hasSize(1);
        assertThat(response.getPageInfo().getTotalElements()).isEqualTo(5L);
    }

    @Test
    @DisplayName("TC-NTF-MQ-010: getChatMessages - Success")
    void getChatMessages_Success() {
        ChatMessageFilter filter = new ChatMessageFilter();
        filter.setJobAdId(1L);
        filter.setCandidateId(200L);
        filter.setPageIndex(Instant.now());
        filter.setPageSize(20);

        ConversationDto dto = new ConversationDto();
        dto.setMessages(new java.util.ArrayList<>(List.of(new com.cvconnect.collection.ChatMessage())));
        
        AggregationResults<ConversationDto> mockResults = new AggregationResults<>(List.of(dto), new Document());
        when(mongoTemplate.aggregate(any(Aggregation.class), eq("conversations"), eq(ConversationDto.class)))
                .thenReturn(mockResults);

        ConversationDto result = mongoQueryService.getChatMessages(filter);

        assertThat(result).isNotNull();
        assertThat(result.getMessagesWithFilter().getData()).hasSize(1);
    }
}
