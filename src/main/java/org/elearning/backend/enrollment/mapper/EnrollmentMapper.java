package org.elearning.backend.enrollment.mapper;

import org.elearning.backend.enrollment.dto.EnrolledCourseDto;
import org.elearning.backend.enrollment.dto.EnrollmentDto;
import org.elearning.backend.enrollment.model.CourseEnrollment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface EnrollmentMapper {
    @Mapping(target = "enrollmentId", source = "id")
    EnrollmentDto toEnrollmentDto(CourseEnrollment enrollment);

    @Mapping(target = "unrollmentId", source = "id")
    EnrolledCourseDto toEnrolledCourseDto(CourseEnrollment enrollment);
    List<EnrolledCourseDto> toEnrolledCourseDtos(List<CourseEnrollment> enrollments);
}
