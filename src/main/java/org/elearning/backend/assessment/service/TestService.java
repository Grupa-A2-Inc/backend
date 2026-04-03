package org.elearning.backend.assessment.service;

import jakarta.transaction.Transactional;
import org.elearning.backend.assessment.dto.QuestionDataForUsersDto;
import org.elearning.backend.assessment.dto.TestEditDto;
import org.elearning.backend.assessment.dto.TestEntityDto;
import org.elearning.backend.assessment.exception.DoesNotExistException;
import org.elearning.backend.assessment.exception.LessonAlreadyHasTestException;
import org.elearning.backend.assessment.exception.TestCannotBePublished;
import org.elearning.backend.assessment.exception.UserHasNoPermissionException;
import org.elearning.backend.assessment.mapper.QuestionOptionMapper;
import org.elearning.backend.assessment.mapper.TestMapper;
import org.elearning.backend.assessment.model.Test;
import org.elearning.backend.assessment.model.TestStatus;
import org.elearning.backend.assessment.repository.QuestionOptionRepository;
import org.elearning.backend.assessment.repository.QuestionRepository;
import org.elearning.backend.assessment.repository.TestRepository;
import org.elearning.backend.content.repository.LessonRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class TestService {
    private final TestMapper testMapper;
    private final TestRepository testRepository;
    private final LessonRepository lessonRepository;
    private final QuestionRepository questionRepository;
    private final QuestionOptionRepository questionOptionRepository;
    private final QuestionOptionMapper questionOptionMapper;

    private static final String LESSON_DOES_NOT_EXIST = "Lesson does not exist";
    private static final String TEST_DOES_NOT_EXIST = "Test does not exist";



    public TestService(TestRepository testRepository, LessonRepository lessonRepository, TestMapper testMapper,
                       QuestionOptionRepository questionOptionRepository, QuestionRepository questionRepository,
                       QuestionOptionMapper questionOptionMapper){
        this.testRepository = testRepository;
        this.lessonRepository = lessonRepository;
        this.testMapper = testMapper;
        this.questionOptionRepository = questionOptionRepository;
        this.questionRepository = questionRepository;
        this.questionOptionMapper = questionOptionMapper;
    }

    /**
     * Creates a new test associated with an existing lesson
     * @param lessonId - the id of the lesson the test is associated with
     * @param modifiableTestData - the data of the new test the teacher can modify
     * @param professorId - the id of the teacher with permission to create a new test
     * @return Dto containing all the information of the created test
     */

    public TestEntityDto createNewTest(UUID lessonId, TestEditDto modifiableTestData, UUID professorId){
        if(!lessonRepository.existsById(lessonId)){
            throw new DoesNotExistException(LESSON_DOES_NOT_EXIST);
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
                orElseThrow(() -> new DoesNotExistException(TEST_DOES_NOT_EXIST));

        if(entity.getQuestions().isEmpty()){
            throw new TestCannotBePublished("The test has no questions and cannot be published");
        }

        testRepository.updateTestStatus(TestStatus.PUBLISHED, testId);
        testRepository.flush();

        return testMapper.toEntityDto(testRepository.findById(testId).orElse(null));

    }

    public TestEntityDto getTestFromLesson(UUID lessonId){
        if(!lessonRepository.existsById(lessonId)){
            throw new DoesNotExistException(LESSON_DOES_NOT_EXIST);
        }
        return testMapper.toEntityDto(testRepository.findByLessonId(lessonId)
                .orElseThrow(() -> new DoesNotExistException(TEST_DOES_NOT_EXIST)));
    }


    public TestEntityDto getTestDetails(UUID testId){
        return testMapper.toEntityDto(testRepository.findById(testId)
                .orElseThrow(() -> new DoesNotExistException(TEST_DOES_NOT_EXIST)));

    }



    @Transactional
    public TestEntityDto updateTest(TestEditDto editableContent, UUID testId){
        if(!testRepository.existsById(testId)){
            throw new DoesNotExistException(TEST_DOES_NOT_EXIST);
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
            throw new DoesNotExistException(TEST_DOES_NOT_EXIST);
        }

        testRepository.deleteTest(testId);

    }

    /**
     * Returns a list of questions with all the options
     * @param testId - the id of the test we want the questions from
     * @return - a list of questions with data the student can see
     */

    private List<QuestionDataForUsersDto> studentGetListOfQuestions(UUID testId){

        List<QuestionDataForUsersDto> questionsWithCorrectOptions = questionRepository.findByTestIdWithOptions(testId).stream()
                .map(QuestionDataForUsersDto::new)
                .toList();

        questionsWithCorrectOptions
                .forEach(instance -> instance.setOptions(questionOptionMapper
                        .toDataForUsersDto(questionOptionRepository.findByQuestionId(instance.getId()))));

        return questionsWithCorrectOptions;

    }

    /**
     * Returns a list of questions that includes the correct answers as well, only a teacher can see
     * @param testId - the id of the test we want the questions from
     * @return - a list of questions that includes the correct answers for each question
     */

    private List<QuestionDataForUsersDto> professorGetListOfQuestions(UUID testId){

        List<QuestionDataForUsersDto> questionsWithCorrectOptions = questionRepository.findByTestIdWithOptions(testId).stream()
                .map(QuestionDataForUsersDto::new)
                .toList();

        questionsWithCorrectOptions
                .forEach(instance -> instance.setOptions(questionOptionMapper
                        .toDataForUsersDto(questionOptionRepository.findByQuestionId(instance.getId()))));

        questionsWithCorrectOptions
                .forEach(instance -> instance.setCorrectAnswers(questionOptionMapper
                        .toDataForUsersDto(questionOptionRepository.findByQuestionIdAndIsCorrectTrue(instance.getId()))));
        return questionsWithCorrectOptions;

    }

    /**
     * Returns a list containing all the data the questions of a given test have. Teachers have access to the list
     * of correct answers as well, IF they are the author of the test. Students have access to the list of question
     * data, minus the correct options ONLY IF they are enrolled to the course related to the question
     * @param testId - the given testId for the wanted test
     * @param userId - the userId of the person calling the method
     * @return Question data that changes depending on the user who calls it, if they have access to it.
     */

    public List<QuestionDataForUsersDto> getListOfQuestions(UUID testId, UUID userId){
        boolean isTeacher = true;

        if(!testRepository.existsById(testId)){
            throw new DoesNotExistException(TEST_DOES_NOT_EXIST);
        }

        /* It's true by default, the scrum masters will write the logic to verify if the user calling is a teacher
          or a student
        */

        if(isTeacher){

            /*
            Might be modified by the scrum masters if they don't think this is a good way to handle it
             */

            if(!testRepository.existsByIdAndByCreatedBy(testId,userId)){
                throw new UserHasNoPermissionException("Only the test's author can access this field");
            }
            return professorGetListOfQuestions(testId);
        }
        return studentGetListOfQuestions(testId);
    }





}
