package org.elearning.backend.analytics.dto.statistics.student;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class MyClassTestAverageDto {
    private Double classAverage;
    private Integer totalStudentCount;
}
