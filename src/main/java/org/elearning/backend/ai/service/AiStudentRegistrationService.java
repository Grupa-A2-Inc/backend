package org.elearning.backend.ai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elearning.backend.ai.exception.AiApiException;
import org.elearning.backend.ai.exception.AiTimeoutException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiStudentRegistrationService {

    private static final int INITIAL_REGISTRATION_ATTEMPT = 1;
    private static final int STUDENT_REGISTRATION_RETRY_COUNT = 2;
    private static final int TOTAL_STUDENT_REGISTRATION_ATTEMPTS =
            INITIAL_REGISTRATION_ATTEMPT + STUDENT_REGISTRATION_RETRY_COUNT;

    private final AiApiClient aiApiClient;

    public void registerStudent(UUID studentId) {
        UUID requestId = UUID.randomUUID();

        for (int attempt = INITIAL_REGISTRATION_ATTEMPT;
             attempt <= TOTAL_STUDENT_REGISTRATION_ATTEMPTS;
             attempt++) {
            try {
                aiApiClient.registerStudent(requestId, studentId);
                return;
            } catch (AiApiException | AiTimeoutException ex) {
                if (attempt == TOTAL_STUDENT_REGISTRATION_ATTEMPTS) {
                    throw ex;
                }

                log.warn(
                        "Student registration in AI failed for studentId={} requestId={} attempt={}/{}. Retrying. Cause={}",
                        studentId,
                        requestId,
                        attempt,
                        TOTAL_STUDENT_REGISTRATION_ATTEMPTS,
                        ex.getMessage()
                );
            }
        }
    }
}
