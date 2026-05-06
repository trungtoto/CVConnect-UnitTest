package com.cvconnect.service.impl;

import com.cvconnect.collection.Notification;
import com.cvconnect.config.socket.SocketHandler;
import com.cvconnect.dto.NotificationDto;
import com.cvconnect.dto.NotificationFilterRequest;
import com.cvconnect.repository.NotificationRepository;
import com.cvconnect.service.MongoQueryService;
import nmquan.commonlib.dto.response.FilterResponse;
import nmquan.commonlib.utils.WebUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;
    
    @Mock
    private MongoQueryService mongoQueryService;

    @Mock
    private SocketHandler socketHandler;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    @Test
    @DisplayName("TC-NTF-NT-001: pushNotify Success")
    void pushNotification_Success() {
        NotificationDto dto = new NotificationDto();
        dto.setReceiverIds(List.of(1L));
        dto.setTitle("Test");
        notificationService.pushNotification(dto);
        verify(notificationRepository, atLeastOnce()).save(any(Notification.class));
    }

    @Test
    @DisplayName("TC-NTF-NT-002: getMyNotifications Success")
    void getMyNotifications_Success() {
        try (MockedStatic<WebUtils> webUtils = mockStatic(WebUtils.class)) {
            webUtils.when(WebUtils::getCurrentUserId).thenReturn(1L);
            NotificationFilterRequest request = new NotificationFilterRequest();
            Page<Notification> mockPage = new PageImpl<>(List.of(new Notification()), PageRequest.of(0, 10), 1);
            when(mongoQueryService.findNotificationWithFilter(any(), any())).thenReturn(mockPage);
            FilterResponse<NotificationDto> results = notificationService.getMyNotifications(request);
            assertThat(results.getData()).isNotEmpty();
        }
    }

    @Test
    @DisplayName("TC-NTF-NT-003: markAsRead Success")
    void markAsRead_Success() {
        try (MockedStatic<WebUtils> webUtils = mockStatic(WebUtils.class)) {
            webUtils.when(WebUtils::getCurrentUserId).thenReturn(1L);
            Notification n = new Notification();
            n.setIsRead(false);
            when(notificationRepository.findByIdAndReceiverId(anyString(), anyLong())).thenReturn(n);
            
            // For unread quantity check
            Page<Notification> mockPage = new PageImpl<>(List.of(), PageRequest.of(0, 1), 0);
            when(mongoQueryService.findNotificationWithFilter(any(), any())).thenReturn(mockPage);

            notificationService.markAsRead("id1");
            assertThat(n.getIsRead()).isTrue();
            verify(notificationRepository).save(n);
        }
    }

    @Test
    @DisplayName("TC-NTF-NT-004: markAllAsRead Success")
    void markAllAsRead_Success() {
        try (MockedStatic<WebUtils> webUtils = mockStatic(WebUtils.class)) {
            webUtils.when(WebUtils::getCurrentUserId).thenReturn(1L);
            Notification n = new Notification();
            n.setIsRead(false);
            when(notificationRepository.findByReceiverIdAndIsReadFalse(anyLong())).thenReturn(List.of(n));

            notificationService.markAllAsRead();
            assertThat(n.getIsRead()).isTrue();
            verify(notificationRepository).saveAll(anyList());
        }
    }

    @Test
    @DisplayName("TC-NTF-NT-005: getQuantityUnread Success")
    void getQuantityUnread_Success() {
        try (MockedStatic<WebUtils> webUtils = mockStatic(WebUtils.class)) {
            webUtils.when(WebUtils::getCurrentUserId).thenReturn(1L);
            Page<Notification> mockPage = new PageImpl<>(List.of(), PageRequest.of(0, 1), 5);
            when(mongoQueryService.findNotificationWithFilter(any(), any())).thenReturn(mockPage);

            Long count = notificationService.getQuantityUnread();
            assertThat(count).isEqualTo(5L);
        }
    }

    @Test
    @DisplayName("TC-NTF-NT-006: pushNotification Null Check")
    void pushNotification_Null() {
        notificationService.pushNotification(null);
        verify(notificationRepository, never()).save(any());
    }
}
