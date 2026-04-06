package org.elearning.backend.enrollment.mapper;

import org.elearning.backend.enrollment.dto.EnrollmentDto;
import org.elearning.backend.enrollment.model.CourseEnrollment;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface EnrollmentMapper {
    EnrollmentDto toEnrollmentDto(CourseEnrollment enrollment);
}
