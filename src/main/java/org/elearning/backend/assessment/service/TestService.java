package org.elearning.backend.assessment.service;

import jakarta.transaction.Transactional;
import org.elearning.backend.assessment.dto.TestEditDto;
import org.elearning.backend.assessment.dto.TestEntityDto;
import org.elearning.backend.assessment.exception.DoesNotExistException;
import org.elearning.backend.assessment.exception.LessonAlreadyHasTestException;
import org.elearning.backend.assessment.exception.TestCannotBePublished;
import org.elearning.backend.assessment.mapper.TestMapper;
import org.elearning.backend.assessment.model.Test;
import org.elearning.backend.assessment.model.TestStatus;
import org.elearning.backend.assessment.repository.TestRepository;
import org.elearning.backend.content.repository.LessonRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class TestService {
    private final TestMapper testMapper;
    private final TestRepository testRepository;
    private final LessonRepository lessonRepository;

    private static final String LESSON_DOES_NOT_EXISTS = "Lesson does not exist";
    private static final String TEST_DOES_NOT_EXISTS = "Test does not exist";


    public TestService(TestRepository testRepository, LessonRepository lessonRepository, TestMapper testMapper){
        this.testRepository = testRepository;
        this.lessonRepository = lessonRepository;
        this.testMapper = testMapper;
    }

    public TestEntityDto createNewTest(UUID lessonId, TestEditDto modifiableTestData, UUID professorId){
        if(!lessonRepository.existsById(lessonId)){
            throw new DoesNotExistException(LESSON_DOES_NOT_EXISTS);
        }

        if(testRepository.lessonHasTest(lessonId)!=0){
            throw new LessonAlreadyHasTestException("Lesson already has a test");
        }

        Test newTest = testMapper.toEntity(modifiableTestData);
        newTest.setLessonId(lessonId);
        newTest.setCreatedBy(professorId);
        newTest.setStatus(TestStatus.DRAFT);

        return testMapper.toEntityDto(testRepository.save(newTest));
    }

    public TestEntityDto publishTest(UUID testId){

        Test entity = testRepository.findById(testId).
                orElseThrow(() -> new DoesNotExistException(TEST_DOES_NOT_EXISTS));

        if(entity.getQuestions().isEmpty()){
            throw new TestCannotBePublished("The test has no questions and cannot be published");
        }

        testRepository.updateTestStatus(TestStatus.PUBLISHED, testId);
        testRepository.flush();

        return testMapper.toEntityDto(testRepository.findById(testId).orElse(null));

    }

    public TestEntityDto getTestFromLesson(UUID lessonId){
        if(!lessonRepository.existsById(lessonId)){
            throw new DoesNotExistException(LESSON_DOES_NOT_EXISTS);
        }
        return testMapper.toEntityDto(testRepository.findByLessonId(lessonId)
                .orElseThrow(() -> new DoesNotExistException(TEST_DOES_NOT_EXISTS)));
    }


    public TestEntityDto getTestDetails(UUID testId){
        return testMapper.toEntityDto(testRepository.findById(testId)
                .orElseThrow(() -> new DoesNotExistException(TEST_DOES_NOT_EXISTS)));

    }



    @Transactional
    public TestEntityDto updateTest(TestEditDto editableContent, UUID testId){
        if(!testRepository.existsById(testId)){
            throw new DoesNotExistException(TEST_DOES_NOT_EXISTS);
        }


        if(editableContent.getTitle()!=null){
            testRepository.updateTestTitle(editableContent.getTitle(), testId);
        }

        if(editableContent.getDescription()!=null){
            testRepository.updateTestDescription(editableContent.getDescription(), testId);
        }
        if(editableContent.getTimeLimitSec()!=null){
            testRepository.updateTestTimeLimitSeconds(editableContent.getTimeLimitSec(), testId);
        }

        if(editableContent.getAiEnabled()!=null){
            testRepository.updateTestAiEnabled(editableContent.getAiEnabled(), testId);
        }

        return testMapper.toEntityDto(testRepository.findById(testId).orElse(null));

    }

    @Transactional
    public void deleteTest(UUID testId){
        if(!testRepository.existsById(testId)){
            throw new DoesNotExistException(TEST_DOES_NOT_EXISTS);
        }

        testRepository.deleteTest(testId);

    }





}
