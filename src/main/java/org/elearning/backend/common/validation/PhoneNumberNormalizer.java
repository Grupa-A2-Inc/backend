package org.elearning.backend.common.validation;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;

public final class PhoneNumberNormalizer {
    private static final PhoneNumberUtil PHONE_NUMBER_UTIL = PhoneNumberUtil.getInstance();
    private static final String DEFAULT_REGION = "RO";
    private static final String INTERNATIONAL_REGION = "ZZ";

    private PhoneNumberNormalizer() {
    }

    public static boolean isValid(String rawPhoneNumber) {
        if (rawPhoneNumber == null || rawPhoneNumber.isBlank()) {
            return true;
        }

        try {
            var parsedPhoneNumber = PHONE_NUMBER_UTIL.parse(rawPhoneNumber, defaultRegionFor(rawPhoneNumber));
            return PHONE_NUMBER_UTIL.isValidNumber(parsedPhoneNumber);
        } catch (NumberParseException ex) {
            return false;
        }
    }

    public static String normalize(String rawPhoneNumber) {
        if (rawPhoneNumber == null || rawPhoneNumber.isBlank()) {
            return null;
        }

        try {
            var parsedPhoneNumber = PHONE_NUMBER_UTIL.parse(rawPhoneNumber, defaultRegionFor(rawPhoneNumber));
            if (!PHONE_NUMBER_UTIL.isValidNumber(parsedPhoneNumber)) {
                throw new IllegalArgumentException("Phone number format is invalid");
            }
            return PHONE_NUMBER_UTIL.format(parsedPhoneNumber, PhoneNumberUtil.PhoneNumberFormat.E164);
        } catch (NumberParseException ex) {
            throw new IllegalArgumentException("Phone number format is invalid", ex);
        }
    }

    private static String defaultRegionFor(String rawPhoneNumber) {
        String trimmedPhoneNumber = rawPhoneNumber.trim();
        return trimmedPhoneNumber.startsWith("+") ? INTERNATIONAL_REGION : DEFAULT_REGION;
    }
}
