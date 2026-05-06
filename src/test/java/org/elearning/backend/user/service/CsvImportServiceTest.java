package org.elearning.backend.user.service;

import org.elearning.backend.role.entity.RoleName;
import org.elearning.backend.subscription.exception.UserLimitExceededException;
import org.elearning.backend.user.dto.response.BulkImportResponse;
import org.elearning.backend.user.dto.response.UserImportResult;
import org.elearning.backend.user.dto.response.UserResponse;
import org.elearning.backend.user.entity.UserStatus;
import org.elearning.backend.user.exception.CsvImportException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CsvImportServiceTest {

    @Mock
    private UserImportService userImportService;

    private CsvImportService csvImportService;

    private final UUID organizationId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        csvImportService = new CsvImportService(userImportService);
    }

    @Test
    void importFromCsv_whenValidCsv_allRowsSucceed() {
        String csv = "email,firstName,lastName,roleName\n" +
                "ion@test.com,Ion,Pop,STUDENT\n" +
                "ana@test.com,Ana,Ionescu,TEACHER\n";

        MockMultipartFile file = buildFile(csv);

        when(userImportService.tryCreateSingleUser(any()))
                .thenReturn(UserImportResult.succeeded(buildUserResponse("ion@test.com")))
                .thenReturn(UserImportResult.succeeded(buildUserResponse("ana@test.com")));

        BulkImportResponse response = csvImportService.importFromCsv(file, organizationId);

        assertThat(response.getTotal()).isEqualTo(2);
        assertThat(response.getSucceeded()).isEqualTo(2);
        assertThat(response.getFailed()).isEqualTo(0);
        verify(userImportService, times(2)).tryCreateSingleUser(any());
    }

    @Test
    void importFromCsv_whenInvalidHeaders_throwsCsvImportException() {
        String csv = "email,nume,prenume,rol\n" +
                "ion@test.com,Ion,Pop,STUDENT\n";

        MockMultipartFile file = buildFile(csv);

        assertThatThrownBy(() -> csvImportService.importFromCsv(file, organizationId))
                .isInstanceOf(CsvImportException.class)
                .hasMessageContaining("Invalid CSV headers");

        verify(userImportService, never()).tryCreateSingleUser(any());
    }

    @Test
    void importFromCsv_whenEmptyFile_throwsCsvImportException() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "users.csv", "text/csv", new byte[0]
        );

        assertThatThrownBy(() -> csvImportService.importFromCsv(file, organizationId))
                .isInstanceOf(CsvImportException.class)
                .hasMessageContaining("must not be empty");
    }

    @Test
    void importFromCsv_whenNotCsvExtension_throwsCsvImportException() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "users.txt", "text/plain", "email,firstName,lastName,roleName\n".getBytes()
        );

        assertThatThrownBy(() -> csvImportService.importFromCsv(file, organizationId))
                .isInstanceOf(CsvImportException.class)
                .hasMessageContaining(".csv extension");
    }

    @Test
    void importFromCsv_whenRowHasWrongColumnCount_markedAsFailed() {
        String csv = "email,firstName,lastName,roleName\n" +
                "ion@test.com,Ion\n";

        MockMultipartFile file = buildFile(csv);

        BulkImportResponse response = csvImportService.importFromCsv(file, organizationId);

        assertThat(response.getTotal()).isEqualTo(1);
        assertThat(response.getFailed()).isEqualTo(1);
        assertThat(response.getResults().get(0).getErrorMessage()).contains("columns");
        verify(userImportService, never()).tryCreateSingleUser(any());
    }

    @Test
    void importFromCsv_whenInvalidRole_rowMarkedAsFailed() {
        String csv = "email,firstName,lastName,roleName\n" +
                "ion@test.com,Ion,Pop,DIRECTOR\n";

        MockMultipartFile file = buildFile(csv);

        BulkImportResponse response = csvImportService.importFromCsv(file, organizationId);

        assertThat(response.getTotal()).isEqualTo(1);
        assertThat(response.getFailed()).isEqualTo(1);
        assertThat(response.getResults().get(0).getErrorMessage()).contains("invalid role");
        verify(userImportService, never()).tryCreateSingleUser(any());
    }

    @Test
    void importFromCsv_whenDuplicateEmailInCsv_secondMarkedAsFailed() {
        String csv = "email,firstName,lastName,roleName\n" +
                "ion@test.com,Ion,Pop,STUDENT\n" +
                "ion@test.com,Ionut,Popa,STUDENT\n";

        MockMultipartFile file = buildFile(csv);

        when(userImportService.tryCreateSingleUser(any()))
                .thenReturn(UserImportResult.succeeded(buildUserResponse("ion@test.com")));

        BulkImportResponse response = csvImportService.importFromCsv(file, organizationId);

        assertThat(response.getTotal()).isEqualTo(2);
        assertThat(response.getSucceeded()).isEqualTo(1);
        assertThat(response.getFailed()).isEqualTo(1);
        assertThat(response.getResults().get(1).getErrorMessage()).contains("duplicate email");
        verify(userImportService, times(1)).tryCreateSingleUser(any());
    }

    @Test
    void importFromCsv_whenEmptyFields_rowMarkedAsFailed() {
        String csv = "email,firstName,lastName,roleName\n" +
                ",Ion,Pop,STUDENT\n";

        MockMultipartFile file = buildFile(csv);

        BulkImportResponse response = csvImportService.importFromCsv(file, organizationId);

        assertThat(response.getTotal()).isEqualTo(1);
        assertThat(response.getFailed()).isEqualTo(1);
        assertThat(response.getResults().get(0).getErrorMessage()).contains("empty required fields");
    }

    @Test
    void importFromCsv_whenQuotaExceeded_rowMarkedAsFailed() {
        String csv = "email,firstName,lastName,roleName\n" +
                "ion@test.com,Ion,Pop,STUDENT\n";

        MockMultipartFile file = buildFile(csv);

        when(userImportService.tryCreateSingleUser(any()))
                .thenReturn(UserImportResult.failed(
                        "ion@test.com",
                        new UserLimitExceededException(organizationId, 5).getMessage()
                ));

        BulkImportResponse response = csvImportService.importFromCsv(file, organizationId);

        assertThat(response.getTotal()).isEqualTo(1);
        assertThat(response.getFailed()).isEqualTo(1);
        assertThat(response.getResults().get(0).getErrorMessage()).contains("5");
    }

    @Test
    void importFromCsv_whenBlankLinesPresent_theyAreIgnored() {
        String csv = "email,firstName,lastName,roleName\n" +
                "\n" +
                "ion@test.com,Ion,Pop,STUDENT\n" +
                "\n";

        MockMultipartFile file = buildFile(csv);

        when(userImportService.tryCreateSingleUser(any()))
                .thenReturn(UserImportResult.succeeded(buildUserResponse("ion@test.com")));

        BulkImportResponse response = csvImportService.importFromCsv(file, organizationId);

        assertThat(response.getTotal()).isEqualTo(1);
        assertThat(response.getSucceeded()).isEqualTo(1);
    }

    private MockMultipartFile buildFile(String content) {
        return new MockMultipartFile(
                "file",
                "users.csv",
                "text/csv",
                content.getBytes()
        );
    }

    private UserResponse buildUserResponse(String email) {
        return new UserResponse(
                UUID.randomUUID(),
                email,
                "Ion",
                "Pop",
                RoleName.STUDENT,
                organizationId,
                UserStatus.PENDING
        );
    }
}