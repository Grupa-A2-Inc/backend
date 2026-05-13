package org.elearning.backend.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.elearning.backend.ai.dto.AiStudentRegistrationRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@org.springframework.test.context.ActiveProfiles("test")
class AiStudentRegistrationRequestTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void exposesExpectedFieldValuesAndSerializesToJson() throws Exception {
        AiStudentRegistrationRequest request = new AiStudentRegistrationRequest(
                "req-123",
                "student-456"
        );

        String json = objectMapper.writeValueAsString(request);

        assertThat(request.requestId()).isEqualTo("req-123");
        assertThat(request.studentId()).isEqualTo("student-456");
        assertThat(json)
                .contains("\"requestId\":\"req-123\"")
                .contains("\"studentId\":\"student-456\"");
    }
}
