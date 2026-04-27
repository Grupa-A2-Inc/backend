package org.elearning.backend.analytics.dto.statistics.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CourseDetailsDto {
    private String courseTitle;
    private Integer totalTestCount;
}
