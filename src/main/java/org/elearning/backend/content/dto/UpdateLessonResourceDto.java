package org.elearning.backend.content.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * DTO for PATCH requests to update a lesson resource.
 * Only includes fields that can be updated (title and url).
 */
@Getter
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateLessonResourceDto {
    private String title;
    private String url;
}
