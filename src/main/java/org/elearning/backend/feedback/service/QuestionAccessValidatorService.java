package org.elearning.backend.feedback.service;

import org.elearning.backend.assessment.repository.QuestionRepository;
import org.elearning.backend.role.entity.RoleName;
import org.elearning.backend.security.access.AccessService;
import org.elearning.backend.security.auth.CustomUserDetails;
import org.elearning.backend.user.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service("questionAccessValidatorService")
public class QuestionAccessValidatorService {
    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;
    private final AccessService accessService;

    public QuestionAccessValidatorService(QuestionRepository questionRepository, UserRepository userRepository, AccessService accessService) {
        this.questionRepository = questionRepository;
        this.userRepository = userRepository;
        this.accessService = accessService;
    }

    public boolean hasStudentAccessToQuestion(Authentication authentication, Integer questionId){
        CustomUserDetails currentUser = accessService.extractCurrentUser(authentication);

        if(currentUser==null){
            return false;
        }
        if(currentUser.getRoleName()!= RoleName.STUDENT){
            return false;
        }
        UUID studentId = currentUser.getUserId();

        if(!questionRepository.existsById(questionId)){
            return false;
        }
        if(!userRepository.existsById(studentId)){
            return false;
        }

        return questionRepository.hasStudentAccessToQuestion(questionId, studentId);
    }
}
