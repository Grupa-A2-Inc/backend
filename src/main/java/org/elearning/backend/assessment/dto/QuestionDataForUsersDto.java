package org.elearning.backend.assessment.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.elearning.backend.assessment.model.Question;
import org.elearning.backend.assessment.model.QuestionType;

import java.math.BigDecimal;
import java.util.List;

/**
 * Used by both teachers and students. The correctAnswers field is not filled unless the user is a teacher.
 */

@Getter
@Setter
@NoArgsConstructor
public class QuestionDataForUsersDto {
    private int id;
    private Integer subjectId;
    private Integer topicId;
    private QuestionType questionType;
    private String content;
    private BigDecimal difficulty;
    private Boolean isActive;
    private List<QuestionOptionsDataDto> options;
    private List<QuestionOptionsDataDto> correctAnswers;

    public QuestionDataForUsersDto(Question question){
        this.id = question.getId();
        this.subjectId = question.getSubjectId();
        this.topicId = question.getTopicId();
        this.questionType = question.getQuestionType();
        this.content = question.getContent();
        this.difficulty = question.getDifficulty();
        this.isActive = question.getIsActive();
    }
}
