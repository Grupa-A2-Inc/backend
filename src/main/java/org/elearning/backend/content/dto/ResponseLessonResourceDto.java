package org.elearning.backend.content.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Data Transfer Object (DTO) for transferring lesson resource data in GET requests.
 * This DTO is used to represent the data of a lesson resource when it is retrieved from the backend.
 */
@Getter
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResponseLessonResourceDto {
    private UUID id;
    private UUID lessonId;
    private String title;
    private String url;
}
