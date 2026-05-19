package org.elearning.backend.common.validation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PhoneNumberNormalizerTest {

    @Test
    void shouldAcceptRomanianLocalNumber() {
        assertThat(PhoneNumberNormalizer.isValid("0722123456")).isTrue();
        assertThat(PhoneNumberNormalizer.normalize("0722123456")).isEqualTo("+40722123456");
    }

    @Test
    void shouldAcceptInternationalRomanianNumber() {
        assertThat(PhoneNumberNormalizer.isValid("+40722123456")).isTrue();
        assertThat(PhoneNumberNormalizer.normalize("+40722123456")).isEqualTo("+40722123456");
    }

    @Test
    void shouldRejectMalformedNumbers() {
        assertThat(PhoneNumberNormalizer.isValid("1")).isFalse();
        assertThat(PhoneNumberNormalizer.isValid("1111111111")).isFalse();
        assertThat(PhoneNumberNormalizer.isValid("12345678901234567890")).isFalse();
        assertThat(PhoneNumberNormalizer.isValid("+40")).isFalse();
        assertThat(PhoneNumberNormalizer.isValid("+407")).isFalse();
    }

    @Test
    void shouldReturnNullForBlankOrNullInput() {
        assertThat(PhoneNumberNormalizer.isValid(null)).isTrue();
        assertThat(PhoneNumberNormalizer.isValid("   ")).isTrue();
        assertThat(PhoneNumberNormalizer.normalize(null)).isNull();
        assertThat(PhoneNumberNormalizer.normalize("   ")).isNull();
    }

    @Test
    void shouldThrowForInvalidNormalizationInput() {
        assertThatThrownBy(() -> PhoneNumberNormalizer.normalize("1111111111"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Phone number format is invalid");
    }

    @Test
    void shouldWrapParseFailuresDuringNormalization() {
        assertThatThrownBy(() -> PhoneNumberNormalizer.normalize("+"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Phone number format is invalid")
                .hasCauseInstanceOf(com.google.i18n.phonenumbers.NumberParseException.class);
    }
}
