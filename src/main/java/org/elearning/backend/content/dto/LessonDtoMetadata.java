package org.elearning.backend.content.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter

//Keeps track of lesson title and order index in a single variable
//Used in the PATCH "/api/lessons/{id}/content" entrypoint

public class LessonDtoMetadata {
    private String title = null;
    private Integer orderIndex = null;

}
