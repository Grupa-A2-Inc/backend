package org.elearning.backend.assessment.dto.assigment_dto;

import lombok.Getter;
import lombok.Setter;
import org.elearning.backend.assessment.model.TestStatus;

import java.time.LocalDateTime;
import java.util.UUID;
@Getter
@Setter
public class TestEntityDto{
    private UUID id;
    private UUID lessonId;
    private UUID createdBy;
    private String title;
    private String description;
    private Integer timeLimitSec;
    private TestStatus status;
    private Boolean aiEnabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
