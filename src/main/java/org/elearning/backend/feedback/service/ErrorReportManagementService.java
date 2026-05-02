package org.elearning.backend.feedback.service;

import lombok.AllArgsConstructor;
import org.elearning.backend.assessment.exception.DoesNotExistException;
import org.elearning.backend.assessment.model.Question;
import org.elearning.backend.assessment.model.QuestionSource;
import org.elearning.backend.assessment.repository.QuestionRepository;
import org.elearning.backend.content.model.Lesson;
import org.elearning.backend.content.repository.LessonRepository;
import org.elearning.backend.feedback.dto.ErrorReportDto;
import org.elearning.backend.feedback.dto.GetErrorReportDto;
import org.elearning.backend.feedback.dto.projections.GetErrorReportProjection;
import org.elearning.backend.feedback.exception.AlreadyResolved;
import org.elearning.backend.feedback.exception.DoesNotOwnTheCourseException;
import org.elearning.backend.feedback.mapper.QuestionErrorReportMapper;
import org.elearning.backend.feedback.model.QuestionErrorReport;
import org.elearning.backend.feedback.model.ReportStatus;
import org.elearning.backend.feedback.repository.QuestionErrorReportRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@AllArgsConstructor
public class ErrorReportManagementService {
    private final QuestionErrorReportRepository questionErrorReportRepository;
    private final QuestionErrorReportMapper questionErrorReportMapper;
    private final QuestionRepository questionRepository;
    private final LessonRepository lessonRepository;

    @Transactional(readOnly = true)
    public Page<GetErrorReportDto> getReports(UUID professorId, ReportStatus status, UUID courseId, Pageable pageable) {
        String statusString = status != null ? status.name() : null;
        Page<GetErrorReportProjection> projections = questionErrorReportRepository.findErrorReportsForProfessor(professorId, statusString, courseId, pageable);

        return projections.map(proj -> {
            ReportStatus reportStatus = ReportStatus.valueOf(proj.getStatus());
            QuestionSource sourceEnum = QuestionSource.valueOf(proj.getQuestionSource());
            return new GetErrorReportDto(
                    proj.getId(),
                    proj.getQuestionId(),
                    proj.getStudentId(),
                    reportStatus,
                    proj.getDescription(),
                    proj.getResolvedAt(),
                    proj.getResolvedBy(),
                    proj.getCreatedAt(),
                    proj.getContent(),
                    sourceEnum,
                    proj.getLessonTitle(),
                    proj.getCourseTitle()
            );
        });
    }

    public ErrorReportDto resolveReport(UUID reportId, UUID professorId) {
        QuestionErrorReport questionErrorReport = questionErrorReportRepository.findById(reportId)
                .orElseThrow(() -> new DoesNotExistException("Report not found with id: " + reportId));
        Optional<Question> questionOptional = questionRepository.findById(questionErrorReport.getQuestionId());
        if (questionOptional.isPresent()) {
            Question question = questionOptional.get();
            Optional<Lesson> lessonOptional = lessonRepository.findById(question.getTest().getLessonId());
            if (lessonOptional.isPresent()) {
                Lesson lesson = lessonOptional.get();
                if (!lesson.getChapter().getCourse().getCreatedBy().equals(professorId)) {
                    throw new DoesNotOwnTheCourseException("User with id " + professorId + " does not own the course for question with id: " + question.getId());
                }
            }
        }
        if (questionErrorReport.getStatus() == ReportStatus.RESOLVED) {
            throw new AlreadyResolved("Report with id " + reportId + " is already resolved");
        }
        questionErrorReport.setStatus(ReportStatus.RESOLVED);
        questionErrorReport.setResolvedAt(java.time.LocalDateTime.now());
        questionErrorReport.setResolvedBy(professorId);
        questionErrorReportRepository.save(questionErrorReport);

        return questionErrorReportMapper.toErrorReportDto(questionErrorReport);
    }
}
