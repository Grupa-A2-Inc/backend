package org.elearning.backend.user.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class CreateUserBulkRequest {
    @NotEmpty(message = "User list must not be empty")
    @Size(max = 100, message = "Cannot import more than 100 users at once")
    @Valid
    private List<CreateUserRequest> users;
}