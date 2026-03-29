package org.elearning.backend.content.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor

//Keeps track of lesson title and content in a single variable
//Used in the POST "/api/lessons/{id}/content" entrypoint

public class LessonDtoPost {
    private String title;
    private String contentMarkdown;
}
