package org.elearning.backend.content.service;

import org.elearning.backend.content.dto.ResponseLessonResourceDto;
import org.elearning.backend.content.dto.UpdateLessonResourceDto;
import org.elearning.backend.content.dto.CreateLessonResourceDto;
import org.elearning.backend.content.exception.InvalidResourceDataException;
import org.elearning.backend.content.exception.LessonNotFoundException;
import org.elearning.backend.content.exception.LessonResourceNotFoundException;
import org.elearning.backend.content.mapper.LessonResourceMapper;
import org.elearning.backend.content.model.Lesson;
import org.elearning.backend.content.model.LessonResource;
import org.elearning.backend.content.repository.LessonRepository;
import org.elearning.backend.content.repository.LessonResourceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class LessonResourceService {
    private final LessonResourceRepository lessonResourceRepository;
    private final LessonRepository lessonRepository;
    private final LessonResourceMapper lessonResourceMapper;

    public LessonResourceService(LessonResourceRepository lessonResourceRepository, LessonRepository lessonRepository, LessonResourceMapper lessonResourceMapper) {
        this.lessonResourceRepository = lessonResourceRepository;
        this.lessonRepository = lessonRepository;
        this.lessonResourceMapper = lessonResourceMapper;
    }

    public ResponseLessonResourceDto createNewLessonResource(CreateLessonResourceDto lessonResourceDTOPost, UUID lessonId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new LessonNotFoundException(lessonId));

        if (lessonResourceDTOPost.getTitle() == null || lessonResourceDTOPost.getUrl() == null) {
            throw new InvalidResourceDataException("Title or URL cannot be null!");
        }

        LessonResource lessonResource = lessonResourceMapper.toLessonResource(lessonResourceDTOPost);
        lessonResource.setLesson(lesson);

        return lessonResourceMapper.toLessonResourceDTOGet(lessonResourceRepository.save(lessonResource));
    }

    @Transactional
    public void deleteLessonResource(UUID resourceId, UUID lessonId) {
        LessonResource resource = lessonResourceRepository.findById(resourceId)
                .orElseThrow(() -> new LessonResourceNotFoundException(resourceId));

        if (!resource.getLesson().getId().equals(lessonId)) {
            throw new LessonResourceNotFoundException(resourceId);
        }

        lessonResourceRepository.delete(resource);
    }

    public List<ResponseLessonResourceDto> getResourcesByLessonId(UUID lessonId) {
        if (!lessonRepository.existsById(lessonId)) {
            throw new LessonNotFoundException(lessonId);
        }
        return lessonResourceMapper.toLessonResourcesDTOGetList(lessonResourceRepository.findByLessonId(lessonId));
    }

    @Transactional
    public ResponseLessonResourceDto updateLessonResource(UUID lessonId, UUID resourceId, UpdateLessonResourceDto updatedResourceDTOPatch) {
        if (!lessonRepository.existsById(lessonId)) {
            throw new LessonNotFoundException(lessonId);
        }

        LessonResource existingResource = lessonResourceRepository.findById(resourceId)
                .orElseThrow(() -> new LessonResourceNotFoundException(resourceId));

        if (!existingResource.getLesson().getId().equals(lessonId)) {
            throw new LessonResourceNotFoundException(resourceId);
        }

        lessonResourceMapper.updateLessonResourceFromDto(updatedResourceDTOPatch, existingResource);
        return lessonResourceMapper.toLessonResourceDTOGet(lessonResourceRepository.save(existingResource));
    }
}