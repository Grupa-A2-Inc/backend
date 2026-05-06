package org.elearning.backend.user.service;

import lombok.RequiredArgsConstructor;
import org.elearning.backend.role.entity.RoleName;
import org.elearning.backend.user.dto.request.CreateUserBulkRequest;
import org.elearning.backend.user.dto.request.CreateUserRequest;
import org.elearning.backend.user.dto.response.BulkImportResponse;
import org.elearning.backend.user.dto.response.UserImportResult;
import org.elearning.backend.user.exception.CsvImportException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CsvImportService {

    private static final String EXPECTED_HEADER = "email,firstName,lastName,roleName";
    private static final Set<String> VALID_ROLES = Set.of(
            RoleName.STUDENT.name(),
            RoleName.TEACHER.name(),
            RoleName.PARENT.name()
    );

    private final UserImportService userImportService;

    public BulkImportResponse importFromCsv(MultipartFile file, UUID organizationId) {
        validateFile(file);

        List<UserImportResult> results = new ArrayList<>();
        Set<String> seenEmails = new HashSet<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String headerLine = reader.readLine();
            if (headerLine == null || !headerLine.trim().equalsIgnoreCase(EXPECTED_HEADER)) {
                throw new CsvImportException(
                        "Invalid CSV headers. Expected: " + EXPECTED_HEADER
                );
            }

            String line;
            int lineNumber = 1;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                String trimmed = line.trim();

                if (trimmed.isEmpty()) {
                    continue;
                }

                String[] parts = trimmed.split(",", -1);

                if (parts.length != 4) {
                    results.add(UserImportResult.failed(
                            "line " + lineNumber,
                            "Row " + lineNumber + " has " + parts.length + " columns, expected 4"
                    ));
                    continue;
                }

                String email = parts[0].trim();
                String firstName = parts[1].trim();
                String lastName = parts[2].trim();
                String roleNameRaw = parts[3].trim();

                if (email.isEmpty() || firstName.isEmpty() || lastName.isEmpty() || roleNameRaw.isEmpty()) {
                    results.add(UserImportResult.failed(
                            email.isEmpty() ? "line " + lineNumber : email,
                            "Row " + lineNumber + " has empty required fields"
                    ));
                    continue;
                }

                if (!VALID_ROLES.contains(roleNameRaw.toUpperCase())) {
                    results.add(UserImportResult.failed(
                            email,
                            "Row " + lineNumber + ": invalid role '" + roleNameRaw + "'"
                    ));
                    continue;
                }

                if (seenEmails.contains(email.toLowerCase())) {
                    results.add(UserImportResult.failed(
                            email,
                            "Row " + lineNumber + ": duplicate email in CSV '" + email + "'"
                    ));
                    continue;
                }

                seenEmails.add(email.toLowerCase());

                CreateUserRequest request = CreateUserRequest.builder()
                        .email(email)
                        .firstName(firstName)
                        .lastName(lastName)
                        .roleName(RoleName.valueOf(roleNameRaw.toUpperCase()))
                        .organizationId(organizationId)
                        .build();

                results.add(userImportService.tryCreateSingleUser(request));
            }

        } catch (CsvImportException e) {
            throw e;
        } catch (Exception e) {
            throw new CsvImportException("Failed to parse CSV file: " + e.getMessage());
        }

        return new BulkImportResponse(results);
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CsvImportException("CSV file must not be empty");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".csv")) {
            throw new CsvImportException("File must have a .csv extension");
        }
    }
}