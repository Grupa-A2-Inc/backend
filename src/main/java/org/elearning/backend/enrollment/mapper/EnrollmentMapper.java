package org.elearning.backend.enrollment.mapper;

import org.elearning.backend.enrollment.dto.EnrolledCourseDto;
import org.elearning.backend.enrollment.dto.EnrollmentDto;
import org.elearning.backend.enrollment.model.CourseEnrollment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface EnrollmentMapper {
    /**
     * Maps a CourseEnrollment entity to an EnrollmentDto.
     *
     * @param enrollment the CourseEnrollment entity to be mapped
     * @return the corresponding EnrollmentDto with the enrollmentId set from the entity's id
     */
    @Mapping(target = "enrollmentId", source = "id")
    EnrollmentDto toEnrollmentDto(CourseEnrollment enrollment);

    /**
     * Maps a CourseEnrollment entity to an EnrolledCourseDto.
     * Do not delete, this is used in the toEnrolledCourseDtos method to map the list of CourseEnrollment entities to a list of EnrolledCourseDto objects.
     *
     * @param enrollment the CourseEnrollment entity to be mapped
     * @return the corresponding EnrolledCourseDto with the unrollmentId set from the entity's id
     */
    @Mapping(target = "unrollmentId", source = "id")
    EnrolledCourseDto toEnrolledCourseDto(CourseEnrollment enrollment);

    /**
     * Maps a list of CourseEnrollment entities to a list of EnrollmentDto objects.
     *
     * @param enrollments the list of CourseEnrollment entities to be mapped
     * @return a list of corresponding EnrollmentDto objects with enrollmentIds set from the entities' ids
     */
    List<EnrolledCourseDto> toEnrolledCourseDtos(List<CourseEnrollment> enrollments);
}
