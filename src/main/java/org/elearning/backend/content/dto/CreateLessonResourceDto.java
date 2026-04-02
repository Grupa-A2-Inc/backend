package org.elearning.backend.content.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * DTO for POST requests to create a new LessonResource.
 * Contains the title and URL of the resource.
 */
@Getter
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateLessonResourceDto {
    private String title;
    private String url;
}
