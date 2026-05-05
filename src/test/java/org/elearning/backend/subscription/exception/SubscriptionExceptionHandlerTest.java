package org.elearning.backend.subscription.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SubscriptionExceptionHandlerTest {

    private final SubscriptionExceptionHandler handler = new SubscriptionExceptionHandler();

    @Test
    void handleSubscriptionNotActive_returnsPaymentRequired() {
        ResponseEntity<Map<String, String>> response =
                handler.handleSubscriptionNotActive(new SubscriptionNotActiveException(UUID.fromString(
                        "11111111-1111-1111-1111-111111111111"
                )));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYMENT_REQUIRED);
        assertThat(response.getBody().get("error")).contains("does not have an active subscription");
    }

    @Test
    void handleUserLimitExceeded_returnsForbidden() {
        ResponseEntity<Map<String, String>> response =
                handler.handleUserLimitExceeded(new UserLimitExceededException(
                        UUID.fromString("22222222-2222-2222-2222-222222222222"),
                        100
                ));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().get("error")).contains("maximum user limit of 100");
    }

    @Test
    void handleClassroomLimitExceeded_returnsForbidden() {
        ResponseEntity<Map<String, String>> response =
                handler.handleClassroomLimitExceeded(new ClassroomLimitExceededException(
                        UUID.fromString("33333333-3333-3333-3333-333333333333"),
                        5
                ));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().get("error")).contains("maximum classroom limit of 5");
    }
}
