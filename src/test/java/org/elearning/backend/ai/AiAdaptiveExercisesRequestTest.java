package org.elearning.backend.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.elearning.backend.ai.dto.AiAdaptiveExercisesRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@org.springframework.test.context.ActiveProfiles("test")
class AiAdaptiveExercisesRequestTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void serializesExpectedJsonFieldNamesAndValues() throws Exception {
        AiAdaptiveExercisesRequest request = new AiAdaptiveExercisesRequest(
                "1ad3147b-3138-4701-beb9-e3afb7f0ef75",
                2,
                7,
                5
        );

        String json = objectMapper.writeValueAsString(request);

        assertThat(json).contains("\"studentId\":\"1ad3147b-3138-4701-beb9-e3afb7f0ef75\"");
        assertThat(json).contains("\"subjectId\":2");
        assertThat(json).contains("\"topicId\":7");
        assertThat(json).contains("\"count\":5");
    }
}
