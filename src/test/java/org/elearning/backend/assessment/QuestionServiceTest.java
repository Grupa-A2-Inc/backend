package org.elearning.backend.assessment;

import jakarta.validation.ValidationException;
import org.elearning.backend.assessment.dto.OptionRequestDto;
import org.elearning.backend.assessment.dto.QuestionRequestDto;
import org.elearning.backend.assessment.dto.QuestionResponseDto;
import org.elearning.backend.assessment.mapper.QuestionMapper;
import org.elearning.backend.assessment.model.Question;
import org.elearning.backend.assessment.model.QuestionType;
import org.elearning.backend.assessment.model.TestStatus;
import org.elearning.backend.assessment.repository.QuestionRepository;
import org.elearning.backend.assessment.repository.TestRepository;
import org.elearning.backend.assessment.service.QuestionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test; // Importul pentru @Test
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuestionServiceTest {

    @Mock
    private TestRepository testRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private QuestionMapper questionMapper; // Mapper-ul trebuie mock-uit și el

    @InjectMocks
    private QuestionService questionService;

    private UUID testId;
    private UUID professorId;
    // Folosim calea completă ca să nu se bată cap în cap cu adnotarea @Test
    private org.elearning.backend.assessment.model.Test mockTest;

    @BeforeEach
    void setUp() {
        testId = UUID.randomUUID();
        professorId = UUID.randomUUID();

        // Construim un Test valid pentru setup-ul de bază
        mockTest = new org.elearning.backend.assessment.model.Test();
        mockTest.setId(testId);
        mockTest.setCreatedBy(professorId);
        mockTest.setStatus(TestStatus.DRAFT);
    }

    // ==========================================
    // 1. CERINȚA: Test: single cu 0 opțiuni corecte → exc.
    // ==========================================
    @Test
    void createQuestion_SingleChoiceZeroCorrectOptions_ThrowsValidationException() {
        // Arrange
        QuestionRequestDto requestDto = new QuestionRequestDto();
        requestDto.setQuestionType(QuestionType.SINGLE_CHOICE);
        requestDto.setOptions(List.of(
                createOptionDto("Option 1", false),
                createOptionDto("Option 2", false)
        ));

        when(testRepository.findById(testId)).thenReturn(Optional.of(mockTest));

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            questionService.createQuestion(testId, requestDto, professorId);
        });

        assertEquals("Single choice should have exactly one correct option.", exception.getMessage());
        verify(questionRepository, never()).save(any());
    }

    // ==========================================
    // 2. CERINȚA: Test: multi cu 1 corect → exc.
    // ==========================================
    @Test
    void createQuestion_MultipleChoiceOneCorrectOption_ThrowsValidationException() {
        // Arrange
        QuestionRequestDto requestDto = new QuestionRequestDto();
        requestDto.setQuestionType(QuestionType.MULTIPLE_CHOICE);
        requestDto.setOptions(List.of(
                createOptionDto("Opt 1", true),
                createOptionDto("Opt 2", false),
                createOptionDto("Opt 3", false)
        ));

        when(testRepository.findById(testId)).thenReturn(Optional.of(mockTest));

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            questionService.createQuestion(testId, requestDto, professorId);
        });

        assertEquals("Multiple choice should have AT LEAST 2 correct options.", exception.getMessage());
        verify(questionRepository, never()).save(any());
    }

    // ==========================================
    // 3. CERINȚA: Test: T/F cu 3 opțiuni → exc.
    // ==========================================
    @Test
    void createQuestion_TrueFalseThreeOptions_ThrowsValidationException() {
        // Arrange
        QuestionRequestDto requestDto = new QuestionRequestDto();
        requestDto.setQuestionType(QuestionType.TRUE_FALSE);
        requestDto.setOptions(List.of(
                createOptionDto("True", true),
                createOptionDto("False", false),
                createOptionDto("Maybe", false)
        ));

        when(testRepository.findById(testId)).thenReturn(Optional.of(mockTest));

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            questionService.createQuestion(testId, requestDto, professorId);
        });

        assertEquals("True/False requires exactly 2 options.", exception.getMessage());
        verify(questionRepository, never()).save(any());
    }

    // ==========================================
    // 4. CERINȚA: Test: creare întrebare pe test PUBLISHED → exc.
    // ==========================================
    @Test
    void createQuestion_TestIsPublished_ThrowsValidationException() {
        // Arrange
        mockTest.setStatus(TestStatus.PUBLISHED); // Schimbăm statusul

        QuestionRequestDto requestDto = new QuestionRequestDto();
        // Nu contează tipul, pentru că excepția va pica pe statusul testului

        when(testRepository.findById(testId)).thenReturn(Optional.of(mockTest));

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            questionService.createQuestion(testId, requestDto, professorId);
        });

        assertEquals("Test is not in DRAFT state and cannot be modified.", exception.getMessage());
        verify(questionRepository, never()).save(any());
    }

    // ==========================================
    // EXTRA 1: Security - Alt profesor încearcă să adauge o întrebare
    // ==========================================
    @Test
    void createQuestion_TestBelongsToAnotherProfessor_ThrowsAccessDeniedException() {
        // Arrange
        UUID hackerProfId = UUID.randomUUID(); // Id diferit față de cel care a creat testul
        QuestionRequestDto requestDto = new QuestionRequestDto();

        when(testRepository.findById(testId)).thenReturn(Optional.of(mockTest));

        // Act & Assert
        AccessDeniedException exception = assertThrows(AccessDeniedException.class, () -> {
            questionService.createQuestion(testId, requestDto, hackerProfId);
        });

        assertEquals("You are not the owner of this test.", exception.getMessage());
        verify(questionRepository, never()).save(any());
    }

    // ==========================================
    // EXTRA 2: Happy Path - Creare întrebare corectă de tip SINGLE CHOICE
    // ==========================================
    @Test
    void createQuestion_ValidSingleChoice_SavesSuccessfully() {
        // Arrange
        QuestionRequestDto requestDto = new QuestionRequestDto();
        requestDto.setContent("Care e rezultatul?");
        requestDto.setQuestionType(QuestionType.SINGLE_CHOICE);
        requestDto.setOptions(List.of(
                createOptionDto("Corect", true),
                createOptionDto("Gresit", false)
        ));

        Question mockQuestionEntity = new Question();
        QuestionResponseDto mockResponseDto = QuestionResponseDto.builder().content("Care e rezultatul?").build();

        // Învățăm Mocks-urile ce să răspundă
        when(testRepository.findById(testId)).thenReturn(Optional.of(mockTest));
        when(questionMapper.toEntity(requestDto)).thenReturn(mockQuestionEntity);
        when(questionRepository.save(mockQuestionEntity)).thenReturn(mockQuestionEntity);
        when(questionMapper.toResponseDto(mockQuestionEntity)).thenReturn(mockResponseDto);

        // Act
        QuestionResponseDto response = questionService.createQuestion(testId, requestDto, professorId);

        // Assert
        assertNotNull(response);
        assertEquals("Care e rezultatul?", response.getContent());
        verify(questionRepository, times(1)).save(mockQuestionEntity); // Verificăm că s-a salvat exact 1 dată
    }

    // ==========================================
    // Metodă ajutătoare pentru generarea DTO-urilor de opțiuni
    // ==========================================
    private OptionRequestDto createOptionDto(String text, boolean isCorrect) {
        OptionRequestDto dto = new OptionRequestDto();
        dto.setText(text);
        dto.setIsCorrect(isCorrect); // Aici se va mapa direct
        dto.setDisplayOrder(1); // Un default ca să nu dea NullPointerException eventual
        return dto;
    }
}
