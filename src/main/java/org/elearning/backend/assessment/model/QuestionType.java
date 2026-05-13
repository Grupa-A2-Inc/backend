package org.elearning.backend.assessment.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum QuestionType {
    SINGLE_CHOICE,
    MULTI_CHOICE,
    TRUE_FALSE;

    @JsonCreator
    public static QuestionType fromValue(String value) {
        if (value == null) {
            return null;
        }

        String normalizedValue = value.trim().toUpperCase();
        if ("MULTIPLE_CHOICE".equals(normalizedValue)) {
            return MULTI_CHOICE;
        }

        return QuestionType.valueOf(normalizedValue);
    }

    @JsonValue
    public String toValue() {
        return name();
    }
}
