package org.elearning.backend.content.service;

import lombok.RequiredArgsConstructor;
import org.elearning.backend.assessment.repository.TestRepository;
import org.elearning.backend.content.exception.CourseNotFoundException;
import org.elearning.backend.content.mapper.CourseFullViewMapper;
import org.elearning.backend.content.mapper.CourseMapper;
import org.elearning.backend.content.model.*;
import org.elearning.backend.content.dto.*;
import org.elearning.backend.content.repository.ChapterRepository;
import org.elearning.backend.content.repository.CourseRepository;
import org.elearning.backend.content.repository.LessonRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;
    private final ChapterRepository chapterRepository;
    private final LessonRepository lessonRepository;
    private final CourseFullViewMapper courseFullViewMapper;
    private final CourseMapper courseMapper;

    private final TestRepository testRepository;

    /**
     * Creates a new course with the provided data.
     * If visibility or status are not provided, they will be set to their default values (PRIVATE and DRAFT, respectively).
     *
     * @param createCourseDto the data for the new course
     * @return the created course in full view format
     */
    @Transactional
    public ResponseCourseFullViewDto createCourse(CreateCourseDto createCourseDto, UUID createdBy) {
        setDefaultCourseProperties(createCourseDto);
        Course course = courseMapper.toCourse(createCourseDto);

        course.setCreatedBy(createdBy);
        course.setVisibility(CourseVisibility.PRIVATE);

        linkChaptersToCourse(course);
        course = courseRepository.saveAndFlush(course);
        return courseFullViewMapper.toCourseFullViewDTO(course, Collections.emptyMap());
    }

    /**
     * Sets default values for course properties if they are not provided in the DTO.
     * If status is null, it will be set to DRAFT.
     *
     * @param createCourseDto the CreateCourseDto containing the course data
     */
    private void setDefaultCourseProperties(CreateCourseDto createCourseDto) {
        if (createCourseDto.getStatus() == null) {
            createCourseDto.setStatus(CourseStatus.DRAFT);
        }
    }

    /**
     * Links chapters to the course and lessons to their respective chapters.
     * This method ensures that the bidirectional relationships between entities are properly set before saving to the database.
     *
     * @param course the Course entity to which chapters and lessons will be linked
     */
    private void linkChaptersToCourse(Course course) {
        if (course.getChapters() == null) {
            return;
        }
        for (Chapter chapter : course.getChapters()) {
            chapter.setCourse(course);
            linkLessonsToChapter(chapter);
        }
    }

    /**
     * Links lessons to their respective chapter and resources to their respective lessons.
     *
     * @param chapter the Chapter entity to which lessons will be linked
     */
    private void linkLessonsToChapter(Chapter chapter) {
        if (chapter.getLessons() == null) {
            return;
        }
        for (Lesson lesson : chapter.getLessons()) {
            lesson.setChapter(chapter);
            linkResourcesToLesson(lesson);
        }
    }

    /**
     * Links resources to their respective lesson.
     *
     * @param lesson the Lesson entity to which resources will be linked
     */
    private void linkResourcesToLesson(Lesson lesson) {
        if (lesson.getLessonResources() == null) {
            return;
        }
        for (LessonResource resource : lesson.getLessonResources()) {
            resource.setLesson(lesson);
        }
    }

    /**
     * Retrieves all published and public courses.
     *
     * @return a list of published and public courses in DTO format
     */
    public List<ResponseCourseDto> getPublicCourses() {
        List<Course> courses = courseRepository.findByStatusAndVisibility(
                CourseStatus.PUBLISHED,
                CourseVisibility.PUBLIC
        );
        return courseMapper.toCourseDtoGetList(courses);
    }

    /**
     * Retrieves all courses created by the given user.
     *
     * @param userId the ID of the user
     * @return a list of courses created by the user in DTO format
     */
    public List<ResponseCourseDto> getMyCourses(UUID userId) {
        List<Course> courses = courseRepository.findByCreatedBy(userId);
        return courseMapper.toCourseDtoGetList(courses);
    }

    /**
     * Updates an existing course with the provided data.
     * If the course does not exist, a CourseNotFoundException will be thrown.
     *
     * @param id the ID of the course to be updated
     * @param updateCourseDto the data for updating the course
     * @return the updated course in DTO format
     */
    @Transactional
    public ResponseCourseDto updateCourse(UUID id, UpdateCourseDto updateCourseDto) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new CourseNotFoundException(id));

        course.setTitle(updateCourseDto.getTitle());
        course.setDescription(updateCourseDto.getDescription());
        course.setCategory(updateCourseDto.getCategory());

        if (updateCourseDto.getStatus() != null) {
            course.setStatus(updateCourseDto.getStatus());
        }

        return courseMapper.toCourseDtoGet(courseRepository.save(course));
    }

    /**
     * Partially updates an existing course with the provided data.
     * Only non-null fields in the UpdateCourseDto will be updated.
     * If the course does not exist, a CourseNotFoundException will be thrown.
     *
     * @param id the ID of the course to be updated
     * @param updateCourseDto the data for partially updating the course
     * @return the updated course in DTO format
     */
    @Transactional
    public ResponseCourseDto patchCourse(UUID id, UpdateCourseDto updateCourseDto) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new CourseNotFoundException(id));
        courseMapper.updateCourseFromDto(updateCourseDto, course);

        return courseMapper.toCourseDtoGet(courseRepository.save(course));
    }

    /**
     * Deletes an existing course by its ID.
     * If the course does not exist, a CourseNotFoundException will be thrown.
     *
     * @param id the ID of the course to be deleted
     */
    public void deleteCourse(UUID id) {
        if (!courseRepository.existsById(id)) {
            throw new CourseNotFoundException(id);
        }

        courseRepository.deleteById(id);
    }

    /**
     * Retrieves a course with all its chapters, lessons, and resources based on the course ID.
     * If the course does not exist, a CourseNotFoundException will be thrown.
     *
     * @param courseId the ID of the course to be retrieved
     * @return the course in full view format, including all chapters, lessons, and resources
     */
    @Transactional(readOnly = true)
    public ResponseCourseFullViewDto getCourseFullView(UUID courseId) {
        Course course = courseRepository.findCourseWithChapters(courseId)
                .orElseThrow(() -> new CourseNotFoundException(courseId));

        chapterRepository.findChaptersWithLessonsByCourseId(courseId);
        lessonRepository.findLessonsWithResourcesByCourseId(courseId);

        List<UUID> lessonIds = course.getChapters().stream()
                .flatMap(chapter -> chapter.getLessons().stream())
                .map(Lesson::getId)
                .toList();

        //lessonId: testId
        Map<UUID, UUID> lessonToTestMap = new java.util.HashMap<>();

        if (!lessonIds.isEmpty()) {
            List<Object[]> testResults = testRepository.findTestIdsByLessonIds(lessonIds);
            for (Object[] row : testResults) {
                UUID lessonId = (UUID) row[0];
                UUID testId = (UUID) row[1];
                lessonToTestMap.put(lessonId, testId);
            }
        }

        return courseFullViewMapper.toCourseFullViewDTO(course, lessonToTestMap);
    }

    public Page<ResponseCourseDto> getPublicCourses(Pageable pageable) {
        return courseRepository.findByStatusAndVisibility(
                CourseStatus.PUBLISHED,
                CourseVisibility.PUBLIC,
                pageable
        ).map(courseMapper::toCourseDtoGet);
    }

    public Page<ResponseCourseDto> getMyCourses(UUID userId, Pageable pageable) {
        return courseRepository.findByCreatedBy(userId, pageable)
                .map(courseMapper::toCourseDtoGet);
    }
}