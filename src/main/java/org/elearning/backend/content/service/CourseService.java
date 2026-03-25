package org.elearning.backend.content.service;

import lombok.RequiredArgsConstructor;
import org.elearning.backend.content.model.Course;
import org.elearning.backend.content.repository.CourseRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;

    //logica pentru POST
    public Course createCourse(Course course) {
        // il salvez in baza de date (dar si returnez obiectul Course)
        return courseRepository.save(course);
    }
}