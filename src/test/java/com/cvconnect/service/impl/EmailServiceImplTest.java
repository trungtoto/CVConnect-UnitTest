package com.cvconnect.service.impl;

import com.cvconnect.dto.EmailConfigDto;
import com.cvconnect.dto.EmailLogDto;
import com.cvconnect.service.EmailConfigService;
import com.cvconnect.service.EmailLogService;
import nmquan.commonlib.dto.SendEmailDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EmailServiceImplTest {

    @Mock
    private EmailConfigService emailConfigService;
    @Mock
    private EmailLogService emailLogService;
    @Mock
    private EmailAsyncServiceImpl emailAsyncServiceImpl;

    @InjectMocks
    private EmailServiceImpl emailService;

    @Test
    @DisplayName("TC-NTF-EM-001: Send Email Success")
    void sendEmail_Success() {
        // [Given]
        SendEmailDto dto = SendEmailDto.builder()
                .orgId(1L)
                .recipients(List.of("test@gmail.com"))
                .subject("Hi")
                .body("Content")
                .build();
        
        EmailConfigDto config = new EmailConfigDto();
        config.setProtocol("smtp");
        config.setHost("smtp.gmail.com");
        config.setPort(587);
        config.setEmail("sender@gmail.com");
        config.setPassword("pass");
        config.setIsSsl(false);

        when(emailConfigService.getByOrgId(anyLong())).thenReturn(config);
        when(emailLogService.save(any(EmailLogDto.class))).thenReturn(1L);

        // [When]
        emailService.sendEmail(dto);

        // [Then] Verify async send was triggered
        verify(emailAsyncServiceImpl).send(any(), any(), anyLong());
        verify(emailLogService).save(any(EmailLogDto.class));
    }
}
