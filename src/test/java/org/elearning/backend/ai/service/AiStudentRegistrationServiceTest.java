package org.elearning.backend.ai.service;

import org.elearning.backend.ai.dto.AiStudentRegistrationResponse;
import org.elearning.backend.ai.exception.AiApiException;
import org.elearning.backend.ai.exception.AiTimeoutException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiStudentRegistrationServiceTest {

    @Mock
    private AiApiClient aiApiClient;

    @InjectMocks
    private AiStudentRegistrationService aiStudentRegistrationService;

    @Test
    void registerStudent_shouldCallAiOnceWhenFirstAttemptSucceeds() {
        UUID studentId = UUID.randomUUID();
        when(aiApiClient.registerStudent(any(UUID.class), eq(studentId)))
                .thenReturn(new AiStudentRegistrationResponse("req-1", "ok", "created"));

        aiStudentRegistrationService.registerStudent(studentId);

        verify(aiApiClient).registerStudent(any(UUID.class), eq(studentId));
    }

    @Test
    void registerStudent_shouldRetryTwiceBeforeSucceeding() {
        UUID studentId = UUID.randomUUID();
        when(aiApiClient.registerStudent(any(UUID.class), eq(studentId)))
                .thenThrow(new AiTimeoutException("timeout-1"))
                .thenThrow(new AiTimeoutException("timeout-2"))
                .thenReturn(new AiStudentRegistrationResponse("req-1", "ok", "created"));

        aiStudentRegistrationService.registerStudent(studentId);

        ArgumentCaptor<UUID> requestIdCaptor = ArgumentCaptor.forClass(UUID.class);
        verify(aiApiClient, times(3)).registerStudent(requestIdCaptor.capture(), eq(studentId));

        List<UUID> requestIds = requestIdCaptor.getAllValues();
        assertThat(requestIds).hasSize(3);
        assertThat(requestIds.get(0)).isEqualTo(requestIds.get(1)).isEqualTo(requestIds.get(2));
    }

    @Test
    void registerStudent_shouldThrowAfterAllAttemptsFail() {
        UUID studentId = UUID.randomUUID();
        when(aiApiClient.registerStudent(any(UUID.class), eq(studentId)))
                .thenThrow(new AiApiException("AI unavailable"));

        assertThatThrownBy(() -> aiStudentRegistrationService.registerStudent(studentId))
                .isInstanceOf(AiApiException.class)
                .hasMessage("AI unavailable");

        verify(aiApiClient, times(3)).registerStudent(any(UUID.class), eq(studentId));
    }
}
