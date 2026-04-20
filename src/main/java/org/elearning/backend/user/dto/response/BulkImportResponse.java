package org.elearning.backend.user.dto.response;

import lombok.Getter;

import java.util.List;

@Getter
public class BulkImportResponse {
    private final int total;
    private final int succeeded;
    private final int failed;
    private final List<UserImportResult> results;

    public BulkImportResponse(List<UserImportResult> results) {
        this.results = results;
        this.total = results.size();
        this.succeeded = (int) results.stream().filter(UserImportResult::isSuccess).count();
        this.failed = this.total - this.succeeded;
    }
}