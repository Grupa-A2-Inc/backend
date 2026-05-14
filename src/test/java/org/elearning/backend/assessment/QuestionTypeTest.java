package org.elearning.backend.assessment;

import org.elearning.backend.assessment.model.QuestionType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class QuestionTypeTest {

    @Test
    void fromValueReturnsNullForNullInput() {
        assertThat(QuestionType.fromValue(null)).isNull();
    }

    @Test
    void fromValueMapsLegacyMultipleChoiceAlias() {
        assertThat(QuestionType.fromValue(" multiple_choice ")).isEqualTo(QuestionType.MULTI_CHOICE);
    }

    @Test
    void toValueReturnsEnumName() {
        assertThat(QuestionType.TRUE_FALSE.toValue()).isEqualTo("TRUE_FALSE");
    }
}
