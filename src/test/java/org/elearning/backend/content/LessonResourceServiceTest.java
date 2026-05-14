package org.elearning.backend.content;

import org.elearning.backend.content.dto.ResponseLessonResourceDto;
import org.elearning.backend.content.dto.UpdateLessonResourceDto;
import org.elearning.backend.content.exception.LessonResourceNotFoundException;
import org.elearning.backend.content.exception.LessonNotFoundException;
import org.elearning.backend.content.mapper.LessonResourceMapper;
import org.elearning.backend.content.repository.LessonRepository;
import org.elearning.backend.content.repository.LessonResourceRepository;
import org.elearning.backend.content.service.LessonResourceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LessonResourceServiceTest {

    @Mock
    private LessonResourceRepository lessonResourceRepository;

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private LessonResourceMapper lessonResourceMapper;

    @InjectMocks
    private LessonResourceService lessonResourceService;

    @Test
    void getResourcesByLessonId_shouldReturnMappedResourcesWhenLessonExists() {
        UUID lessonId = UUID.randomUUID();
        List<ResponseLessonResourceDto> expected = List.of(
                new ResponseLessonResourceDto(UUID.randomUUID(), lessonId, "Slides", "https://example.com/slides")
        );

        when(lessonRepository.existsById(lessonId)).thenReturn(true);
        when(lessonResourceRepository.findByLessonId(lessonId)).thenReturn(List.of());
        when(lessonResourceMapper.toLessonResourcesDTOGetList(List.of())).thenReturn(expected);

        List<ResponseLessonResourceDto> result = lessonResourceService.getResourcesByLessonId(lessonId);

        assertThat(result).isEqualTo(expected);
        verify(lessonResourceRepository).findByLessonId(lessonId);
    }

    @Test
    void getResourcesByLessonId_shouldThrowWhenLessonDoesNotExist() {
        UUID lessonId = UUID.randomUUID();
        when(lessonRepository.existsById(lessonId)).thenReturn(false);

        assertThatThrownBy(() -> lessonResourceService.getResourcesByLessonId(lessonId))
                .isInstanceOf(LessonNotFoundException.class);
    }

    @Test
    void createNewLessonResource_shouldThrowWhenLessonDoesNotExist() {
        UUID lessonId = UUID.randomUUID();
        var request = new org.elearning.backend.content.dto.CreateLessonResourceDto("Slides", "https://example.com");
        when(lessonRepository.findById(lessonId)).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> lessonResourceService.createNewLessonResource(request, lessonId))
                .isInstanceOf(LessonNotFoundException.class);
    }

    @Test
    void deleteLessonResource_shouldThrowWhenResourceDoesNotExist() {
        UUID lessonId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        when(lessonResourceRepository.findById(resourceId)).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> lessonResourceService.deleteLessonResource(resourceId, lessonId))
                .isInstanceOf(LessonResourceNotFoundException.class);
    }

    @Test
    void updateLessonResource_shouldThrowWhenResourceDoesNotExist() {
        UUID lessonId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        UpdateLessonResourceDto request = new UpdateLessonResourceDto("Updated", "https://example.com/updated");
        when(lessonRepository.existsById(lessonId)).thenReturn(true);
        when(lessonResourceRepository.findById(resourceId)).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> lessonResourceService.updateLessonResource(lessonId, resourceId, request))
                .isInstanceOf(LessonResourceNotFoundException.class);
    }
}
