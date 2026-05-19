package org.elearning.backend.organization.dto.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OrganizationRequestDtoTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void tearDownValidator() {
        validatorFactory.close();
    }

    @Test
    void createOrganizationRequest_builderShouldPopulateAllFields() {
        UUID ownerId = UUID.randomUUID();

        CreateOrganizationRequest request = CreateOrganizationRequest.builder()
                .name("Org")
                .country("Romania")
                .city("Bucharest")
                .organizationType("School")
                .address("Street 1")
                .phoneNumber("+40722123456")
                .ownerId(ownerId)
                .build();

        assertThat(request.getName()).isEqualTo("Org");
        assertThat(request.getCountry()).isEqualTo("Romania");
        assertThat(request.getCity()).isEqualTo("Bucharest");
        assertThat(request.getOrganizationType()).isEqualTo("School");
        assertThat(request.getAddress()).isEqualTo("Street 1");
        assertThat(request.getPhoneNumber()).isEqualTo("+40722123456");
        assertThat(request.getOwnerId()).isEqualTo(ownerId);
    }

    @Test
    void createOrganizationRequest_noArgsConstructorShouldAllowFieldAccessThroughReflection() {
        UUID ownerId = UUID.randomUUID();
        CreateOrganizationRequest request = new CreateOrganizationRequest();

        ReflectionTestUtils.setField(request, "name", "Org");
        ReflectionTestUtils.setField(request, "country", "Romania");
        ReflectionTestUtils.setField(request, "city", "Cluj");
        ReflectionTestUtils.setField(request, "organizationType", "University");
        ReflectionTestUtils.setField(request, "address", "Street 2");
        ReflectionTestUtils.setField(request, "phoneNumber", "+40722123456");
        ReflectionTestUtils.setField(request, "ownerId", ownerId);

        assertThat(request.getName()).isEqualTo("Org");
        assertThat(request.getCountry()).isEqualTo("Romania");
        assertThat(request.getCity()).isEqualTo("Cluj");
        assertThat(request.getOrganizationType()).isEqualTo("University");
        assertThat(request.getAddress()).isEqualTo("Street 2");
        assertThat(request.getPhoneNumber()).isEqualTo("+40722123456");
        assertThat(request.getOwnerId()).isEqualTo(ownerId);
    }

    @Test
    void createOrganizationRequest_allArgsConstructorShouldPopulateAllFields() {
        UUID ownerId = UUID.randomUUID();

        CreateOrganizationRequest request = new CreateOrganizationRequest(
                "Org",
                "Romania",
                "Iasi",
                "School",
                "Street 3",
                "+40722123456",
                ownerId
        );

        assertThat(request.getName()).isEqualTo("Org");
        assertThat(request.getCountry()).isEqualTo("Romania");
        assertThat(request.getCity()).isEqualTo("Iasi");
        assertThat(request.getOrganizationType()).isEqualTo("School");
        assertThat(request.getAddress()).isEqualTo("Street 3");
        assertThat(request.getPhoneNumber()).isEqualTo("+40722123456");
        assertThat(request.getOwnerId()).isEqualTo(ownerId);
    }

    @Test
    void createOrganizationRequest_shouldValidateRequiredFieldsAndPhoneNumber() {
        CreateOrganizationRequest request = new CreateOrganizationRequest(
                "",
                "",
                "",
                "",
                "Street 4",
                "invalid-phone",
                UUID.randomUUID()
        );

        Set<ConstraintViolation<CreateOrganizationRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains(
                        "Organization name is required",
                        "Country is required",
                        "City is required",
                        "Organization type is required",
                        "Phone number format is invalid"
                );
    }

    @Test
    void updateOrganizationRequest_builderShouldPopulateAllFields() {
        UpdateOrganizationRequest request = UpdateOrganizationRequest.builder()
                .name("Updated Org")
                .country("Romania")
                .city("Timisoara")
                .organizationType("Company")
                .address("Street 5")
                .phoneNumber("+40722123456")
                .build();

        assertThat(request.getName()).isEqualTo("Updated Org");
        assertThat(request.getCountry()).isEqualTo("Romania");
        assertThat(request.getCity()).isEqualTo("Timisoara");
        assertThat(request.getOrganizationType()).isEqualTo("Company");
        assertThat(request.getAddress()).isEqualTo("Street 5");
        assertThat(request.getPhoneNumber()).isEqualTo("+40722123456");
    }

    @Test
    void updateOrganizationRequest_noArgsConstructorShouldAllowFieldAccessThroughReflection() {
        UpdateOrganizationRequest request = new UpdateOrganizationRequest();

        ReflectionTestUtils.setField(request, "name", "Updated Org");
        ReflectionTestUtils.setField(request, "country", "Romania");
        ReflectionTestUtils.setField(request, "city", "Brasov");
        ReflectionTestUtils.setField(request, "organizationType", "School");
        ReflectionTestUtils.setField(request, "address", "Street 6");
        ReflectionTestUtils.setField(request, "phoneNumber", "+40722123456");

        assertThat(request.getName()).isEqualTo("Updated Org");
        assertThat(request.getCountry()).isEqualTo("Romania");
        assertThat(request.getCity()).isEqualTo("Brasov");
        assertThat(request.getOrganizationType()).isEqualTo("School");
        assertThat(request.getAddress()).isEqualTo("Street 6");
        assertThat(request.getPhoneNumber()).isEqualTo("+40722123456");
    }

    @Test
    void updateOrganizationRequest_allArgsConstructorShouldPopulateAllFields() {
        UpdateOrganizationRequest request = new UpdateOrganizationRequest(
                "Updated Org",
                "Romania",
                "Constanta",
                "School",
                "Street 7",
                "+40722123456"
        );

        assertThat(request.getName()).isEqualTo("Updated Org");
        assertThat(request.getCountry()).isEqualTo("Romania");
        assertThat(request.getCity()).isEqualTo("Constanta");
        assertThat(request.getOrganizationType()).isEqualTo("School");
        assertThat(request.getAddress()).isEqualTo("Street 7");
        assertThat(request.getPhoneNumber()).isEqualTo("+40722123456");
    }

    @Test
    void updateOrganizationRequest_shouldValidateRequiredFieldsAndPhoneNumber() {
        UpdateOrganizationRequest request = new UpdateOrganizationRequest(
                "",
                "",
                "",
                "",
                "Street 8",
                "invalid-phone"
        );

        Set<ConstraintViolation<UpdateOrganizationRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains(
                        "Organization name is required",
                        "Country is required",
                        "City is required",
                        "Organization type is required",
                        "Phone number format is invalid"
                );
    }
}
