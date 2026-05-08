package org.elearning.backend.feedback.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elearning.backend.assessment.model.Question;
import org.elearning.backend.assessment.model.QuestionSource;
import org.elearning.backend.assessment.repository.QuestionRepository;
import org.elearning.backend.feedback.model.QuestionErrorReport;
import org.elearning.backend.feedback.repository.QuestionErrorReportRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ErrorReportRoutingService {
    private final QuestionErrorReportRepository questionErrorReportRepository;
    private final QuestionRepository questionRepository;

    public void route(UUID reportId) {
        QuestionErrorReport report = questionErrorReportRepository.findById(reportId)
                .orElse(null);
        if (report == null) {
            log.warn("[ERROR REPORT ROUTING] Raport cu id {} nu a fost gasit pentru rutare.", reportId);
            return;
        }
        Question question = questionRepository.findById(report.getQuestionId()).orElse(null);
        if (question == null) {
            log.warn("[ERROR REPORT ROUTING] Intrebarea cu id {} raportata in raportul {} nu a fost gasita pentru rutare.", report.getQuestionId(), reportId);
            return;
        }
        QuestionSource source = question.getSource();
        if (source == QuestionSource.AI_GENERATED) {
            log.info("[AI ROUTING] Raport {} rutat la AI team - question {} este AI_GENERATED", reportId, question.getId());
        }
    }
}
