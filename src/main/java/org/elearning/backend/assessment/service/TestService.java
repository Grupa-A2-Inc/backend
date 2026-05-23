package org.elearning.backend.assessment.service;

import jakarta.transaction.Transactional;
import org.elearning.backend.assessment.dto.question_dto.QuestionDataForUsersDto;
import org.elearning.backend.assessment.dto.assigment_dto.TestEditDto;
import org.elearning.backend.assessment.dto.assigment_dto.TestEntityDto;
import org.elearning.backend.assessment.exception.*;
import org.elearning.backend.assessment.mapper.QuestionOptionMapper;
import org.elearning.backend.assessment.mapper.TestMapper;
import org.elearning.backend.assessment.model.Question;
import org.elearning.backend.assessment.model.QuestionOption;
import org.elearning.backend.assessment.model.Test;
import org.elearning.backend.assessment.model.TestStatus;
import org.elearning.backend.assessment.repository.QuestionOptionRepository;
import org.elearning.backend.assessment.repository.QuestionRepository;
import org.elearning.backend.assessment.repository.TestRepository;
import org.elearning.backend.content.repository.LessonRepository;
import org.elearning.backend.role.entity.RoleName;
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
    private static final String TEST_MUST_BE_DRAFT = "Test must be a draft for this operation";
    private static final String ONLY_PUBLISHED_TESTS_CAN_BE_CLONED = "Only published tests can be converted to an editable draft";



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

        if(testRepository.existsByLessonId(lessonId)){
            throw new LessonAlreadyHasTestException("Lesson already has a test");
        }

        Test newTest = testMapper.toEntity(modifiableTestData);
        newTest.setLessonId(lessonId);
        newTest.setCreatedBy(professorId);
        newTest.setVersion(1);
        newTest.setPreviousVersionId(null);
        newTest.setStatus(TestStatus.DRAFT);

        return testMapper.toEntityDto(testRepository.save(newTest));
    }

    @Transactional
    public TestEntityDto publishTest(UUID testId){

        Test entity = testRepository.findById(testId).
                orElseThrow(() -> new DoesNotExistException(TEST_DOES_NOT_EXIST));

        if(entity.getQuestions().isEmpty()){
            throw new TestCannotBePublished("The test has no questions and cannot be published");
        }

        if(entity.getStatus() != TestStatus.DRAFT){
            if(entity.getStatus() == TestStatus.PUBLISHED){
                throw new AlreadyPublishedException("The test was already published");
            }
            throw new TestMustBeDraftException(TEST_MUST_BE_DRAFT);
        }

        Test currentPublished = testRepository.findTopByLessonIdAndStatusOrderByVersionDesc(entity.getLessonId(), TestStatus.PUBLISHED)
                .orElse(null);
        if (currentPublished != null) {
            currentPublished.setStatus(TestStatus.SUPERSEDED);
            testRepository.saveAndFlush(currentPublished);
        }

        entity.setStatus(TestStatus.PUBLISHED);
        testRepository.saveAndFlush(entity);

        return testMapper.toEntityDto(entity);

    }

    public TestEntityDto getTestFromLesson(UUID lessonId){
        return getTestFromLesson(lessonId, RoleName.TEACHER);
    }

    public TestEntityDto getTestFromLesson(UUID lessonId, RoleName roleName){
        if(!lessonRepository.existsById(lessonId)){
            throw new DoesNotExistException(LESSON_DOES_NOT_EXIST);
        }

        Test selectedTest;

        if (roleName == RoleName.TEACHER) {
            selectedTest = testRepository.findTopByLessonIdAndStatusOrderByVersionDesc(lessonId, TestStatus.DRAFT)
                    .or(() -> testRepository.findTopByLessonIdAndStatusOrderByVersionDesc(lessonId, TestStatus.PUBLISHED))
                    .or(() -> testRepository.findTopByLessonIdOrderByVersionDesc(lessonId))
                    .orElseThrow(() -> new DoesNotExistException(TEST_DOES_NOT_EXIST));
        } else {
            selectedTest = testRepository.findTopByLessonIdAndStatusOrderByVersionDesc(lessonId, TestStatus.PUBLISHED)
                    .orElseThrow(() -> new DoesNotExistException(TEST_DOES_NOT_EXIST));
        }

        return testMapper.toEntityDto(selectedTest);
    }


    public TestEntityDto getTestDetails(UUID testId){
        return testMapper.toEntityDto(testRepository.findById(testId)
                .orElseThrow(() -> new DoesNotExistException(TEST_DOES_NOT_EXIST)));

    }



    /**
     * Updates modifiable fields of a draft test and returns the updated test DTO.
     *
     * Only non-null fields in {@code editableContent} are applied; updates are permitted only when the test is in draft status.
     *
     * @param editableContent DTO containing fields to update (null fields are ignored)
     * @param testId          identifier of the test to update
     * @return                the updated test mapped to a {@code TestEntityDto}
     * @throws DoesNotExistException      if no test exists with the given {@code testId}
     * @throws TestMustBeDraftException   if the test exists but its status is not draft
     */
    @Transactional
    public TestEntityDto updateTest(TestEditDto editableContent, UUID testId){
        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new DoesNotExistException(TEST_DOES_NOT_EXIST));

        if(!test.getStatus().equals(TestStatus.DRAFT)){
            throw new TestMustBeDraftException(TEST_MUST_BE_DRAFT);
        }

        if(editableContent.getTitle() != null)
            test.setTitle(editableContent.getTitle());
        if(editableContent.getDescription() != null)
            test.setDescription(editableContent.getDescription());
        if(editableContent.getTimeLimitSec() != null)
            test.setTimeLimitSec(editableContent.getTimeLimitSec());
        if(editableContent.getAiEnabled() != null)
            test.setAiEnabled(editableContent.getAiEnabled());

        return testMapper.toEntityDto(testRepository.save(test));
    }

    @Transactional
    public TestEntityDto ensureEditableDraft(UUID testId) {
        Test source = testRepository.findById(testId)
                .orElseThrow(() -> new DoesNotExistException(TEST_DOES_NOT_EXIST));

        if (source.getStatus() == TestStatus.DRAFT) {
            return testMapper.toEntityDto(source);
        }

        Test existingDraft = testRepository.findTopByLessonIdAndStatusOrderByVersionDesc(source.getLessonId(), TestStatus.DRAFT)
                .orElse(null);
        if (existingDraft != null) {
            return testMapper.toEntityDto(existingDraft);
        }

        if (source.getStatus() != TestStatus.PUBLISHED) {
            throw new TestVersionConflictException(ONLY_PUBLISHED_TESTS_CAN_BE_CLONED);
        }

        Test draft = cloneTestAsDraft(source);
        return testMapper.toEntityDto(draft);
    }

    /**
     * Deletes the test with the given id only if it is in draft status.
     *
     * @param testId the UUID of the test to delete
     * @throws TestNotPublishedException if no test exists with the provided id
     * @throws TestMustBeDraftException if the test exists but its status is not DRAFT
     */
    @Transactional
    public void deleteTest(UUID testId){
        Test test = testRepository.findById(testId)
                        .orElseThrow(() -> new TestNotPublishedException(TEST_DOES_NOT_EXIST));

        if(!test.getStatus().equals(TestStatus.DRAFT)){
            throw new TestMustBeDraftException(TEST_MUST_BE_DRAFT);
        }

        testRepository.deleteTest(testId);

    }

    private Test cloneTestAsDraft(Test source) {
        Test draft = new Test();
        draft.setLessonId(source.getLessonId());
        draft.setCreatedBy(source.getCreatedBy());
        Integer maxVersion = testRepository.findMaxVersionByLessonId(source.getLessonId());
        draft.setVersion((maxVersion == null ? 0 : maxVersion) + 1);
        draft.setPreviousVersionId(source.getId());
        draft.setTitle(source.getTitle());
        draft.setDescription(source.getDescription());
        draft.setTimeLimitSec(source.getTimeLimitSec());
        draft.setAiEnabled(source.getAiEnabled());
        draft.setStatus(TestStatus.DRAFT);

        Test savedDraft = testRepository.save(draft);
        cloneQuestions(source, savedDraft);
        return savedDraft;
    }

    private void cloneQuestions(Test source, Test draft) {
        List<Question> sourceQuestions = questionRepository.findByTestIdWithOptions(source.getId());
        if (sourceQuestions.isEmpty()) {
            return;
        }

        List<Question> clonedQuestions = sourceQuestions.stream()
                .map(sourceQuestion -> cloneQuestion(sourceQuestion, draft))
                .toList();

        questionRepository.saveAll(clonedQuestions);
    }

    private Question cloneQuestion(Question sourceQuestion, Test draft) {
        Question clonedQuestion = new Question();
        clonedQuestion.setTest(draft);
        clonedQuestion.setSubjectId(sourceQuestion.getSubjectId());
        clonedQuestion.setTopicId(sourceQuestion.getTopicId());
        clonedQuestion.setQuestionType(sourceQuestion.getQuestionType());
        clonedQuestion.setContent(sourceQuestion.getContent());
        clonedQuestion.setDifficulty(sourceQuestion.getDifficulty());
        clonedQuestion.setIsActive(sourceQuestion.getIsActive());
        clonedQuestion.setSource(sourceQuestion.getSource());

        List<QuestionOption> clonedOptions = sourceQuestion.getOptions().stream()
                .map(sourceOption -> cloneOption(sourceOption, clonedQuestion))
                .toList();
        clonedQuestion.setOptions(clonedOptions);

        return clonedQuestion;
    }

    private QuestionOption cloneOption(QuestionOption sourceOption, Question clonedQuestion) {
        QuestionOption clonedOption = new QuestionOption();
        clonedOption.setQuestion(clonedQuestion);
        clonedOption.setText(sourceOption.getText());
        clonedOption.setDisplayOrder(sourceOption.getDisplayOrder());
        clonedOption.setIsCorrect(sourceOption.getIsCorrect());
        return clonedOption;
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
         * Retrieve question data for a test, with correct answers included when the caller is a teacher.
         *
         * @param testId   the identifier of the test whose questions are requested
         * @param roleName the role of the caller used to determine the level of information returned
         * @return a list of QuestionDataForUsersDto representing the test questions; for callers with teacher role the returned items include correct answers
         * @throws DoesNotExistException if no test exists with the given id
         * @throws UserHasNoPermissionException if the caller's role is not permitted to view the questions
         */

    public List<QuestionDataForUsersDto> getListOfQuestions(UUID testId, RoleName roleName){

        if(!testRepository.existsById(testId)){
            throw new DoesNotExistException(TEST_DOES_NOT_EXIST);
        }

        if(roleName.equals(RoleName.TEACHER)){
            return professorGetListOfQuestions(testId);
        }
        else{
            throw new UserHasNoPermissionException("User must be the teacher of the course to view this");
        }

    }





}
