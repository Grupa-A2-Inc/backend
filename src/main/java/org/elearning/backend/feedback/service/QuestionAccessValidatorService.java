package org.elearning.backend.feedback.service;

import org.elearning.backend.assessment.repository.QuestionRepository;
import org.elearning.backend.user.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class QuestionAccessValidatorService {
    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;

    public QuestionAccessValidatorService(QuestionRepository questionRepository, UserRepository userRepository) {
        this.questionRepository = questionRepository;
        this.userRepository = userRepository;
    }

    public boolean hasStudentAccessToQuestion(Integer questionId, UUID studentId){
        if(!questionRepository.existsById(questionId)){
            return false;
        }
        if(!userRepository.existsById(studentId)){
            return false;
        }
        return questionRepository.hasStudentAccessToQuestion(questionId, studentId);
    }
}
