package org.elearning.backend.analytics.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
// SWAGGER ADDED
import io.swagger.v3.oas.annotations.tags.Tag;
import org.elearning.backend.analytics.dto.statistics.student.MySummaryDataDto;
import org.elearning.backend.analytics.dto.statistics.teacher.ClassAverageDto;
import org.elearning.backend.analytics.dto.statistics.student.MyTestStatsDto;
import org.elearning.backend.analytics.dto.statistics.teacher.StudentAverageDto;
import org.elearning.backend.analytics.service.AnalyticsQueryService;
import org.elearning.backend.analytics.service.StudentsStatsService;

import org.elearning.backend.security.auth.CustomUserDetails;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.parameters.P;

import org.springframework.data.domain.Pageable;

import java.util.UUID;

// SWAGGER ADDED
@Tag(name = "Analytics & Stats", description = "Course and test analytics for teachers and students")
@RestController
@RequestMapping("/api/v1")
public class AnalyticsAndStatsController {

    private static final String OK = "200";

    private static final String FORBIDDEN = "403";
    private static final String NOT_FOUND = "404";

    private final AnalyticsQueryService analyticsQueryService;
    private final StudentsStatsService studentsStatsService;

    /**
     * Create a new AnalyticsAndStatsController wired with the services required to serve analytics and student statistics endpoints.
     *
     * @param analyticsQueryService service providing analytics queries such as class averages and student averages
     * @param studentsStatsService  service providing per-student statistics and summary data
     */
    public AnalyticsAndStatsController(AnalyticsQueryService analyticsQueryService, StudentsStatsService studentsStatsService) {
        this.analyticsQueryService = analyticsQueryService;
        this.studentsStatsService = studentsStatsService;
    }

    /**
     * Retrieve class average statistics for a specific test.
     *
     * @param testId UUID of the test to fetch class average for
     * @return ClassAverageDto containing aggregated class average metrics for the authenticated teacher's class
     */
    @Operation(summary = "Get class average",
            description = "A teacher can get the results of their class at a given test they created.")
    @ApiResponse(responseCode = OK, description = "Data returned")
    @ApiResponse(responseCode = NOT_FOUND, description = "Test does not exist")
    @ApiResponse(responseCode = FORBIDDEN, description = "User does not have the permissions to view test data")

    @GetMapping("/tests/{testId}/analytics/class-average")
    @PreAuthorize("@accessService.canViewTest(authentication,#id)")
    public ResponseEntity<ClassAverageDto> getClassAverage(
            @P("id") @PathVariable UUID testId,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        UUID userId = currentUser.getUserId();

        return ResponseEntity.ok().body(analyticsQueryService.getClassAverage(testId, userId));
    }

    /**
     * Retrieve paginated per-student average statistics for a course.
     *
     * @param courseId the UUID of the course to fetch student averages for
     * @param pageable pagination and sorting information for the returned page
     * @return a page of StudentAverageDto containing average statistics for each student in the requested course page
     */
    @Operation(summary = "Get individual student data",
            description = "A teacher can get individual results from each student at a course from a given page.")
    @ApiResponse(responseCode = OK, description = "Data returned")
    @ApiResponse(responseCode = NOT_FOUND, description = "Course does not exist")
    @ApiResponse(responseCode = FORBIDDEN, description = "User does not have the permissions to view student data")

    @GetMapping("/courses/{courseId}/analytics/student-averages")
    @PreAuthorize("@accessService.canViewCourseFullView(authentication,#id)")
    public ResponseEntity<Page<StudentAverageDto>> getStudentAverages(
            @P("id") @PathVariable UUID courseId,
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @ParameterObject Pageable pageable) {
        UUID userId = currentUser.getUserId();
        return ResponseEntity.ok().body(analyticsQueryService.getStudentAverages(courseId, userId, pageable));
    }

    /**
     * Retrieve the authenticated student's statistics for the specified test.
     *
     * @param testId the UUID of the test
     * @param currentUser the authenticated user's details
     * @return the student's test statistics as a MyTestStatsDto
     */
    @Operation(summary = "Get personal test data",
            description = "A student can view their own statistics from a given test.")
    @ApiResponse(responseCode = OK, description = "Data returned")
    @ApiResponse(responseCode = NOT_FOUND, description = "Test does not exist")
    @ApiResponse(responseCode = FORBIDDEN, description = "User does not have the permissions to view data from given test")

    @GetMapping("/students/me/tests/{testId}/stats")
    @PreAuthorize("@accessService.canViewMyBestTestResult(authentication,#id)")
    public ResponseEntity<MyTestStatsDto> getMyTestStats(
            @P("id") @PathVariable UUID testId,
            @AuthenticationPrincipal CustomUserDetails currentUser){
        UUID userId = currentUser.getUserId();
        return ResponseEntity.ok().body(studentsStatsService.getMyTestStats(userId, testId));
    }

    /**
     * Retrieve the authenticated student's summary statistics for a specific course.
     *
     * The returned summary includes total tests taken, total tests passed, best score,
     * average score, worst score, the three lessons where the student is struggling the most,
     * and the last five attempts.
     *
     * @param courseId the UUID of the course to fetch statistics for
     * @return a MySummaryDataDto containing the student's course summary (totals, scores, top-three struggling lessons, and last five attempts)
     */
    @Operation(summary = "Get personal course data",
            description = "A student can view their own statistics from course involving the total numbers of tests taken," +
                    " total tests passed, best score, average score, worst score, three lessons where the student is the" +
                    " most struggling at and the last five attempts.")
    @ApiResponse(responseCode = OK, description = "Data returned")
    @ApiResponse(responseCode = NOT_FOUND, description = "Course does not exist")
    @ApiResponse(responseCode = FORBIDDEN, description = "Student not enrolled to the course")
    @GetMapping("/students/me/courses/{courseId}/stats")
    @PreAuthorize("@accessService.canEnrollInCourse(authentication,#id)")
    public ResponseEntity<MySummaryDataDto> getMySummaryData(
            @P("id") @PathVariable UUID courseId,
            @AuthenticationPrincipal CustomUserDetails currentUser){
        UUID userId = currentUser.getUserId();
        return ResponseEntity.ok().body(studentsStatsService.getMySummaryData(userId, courseId));
    }


}
