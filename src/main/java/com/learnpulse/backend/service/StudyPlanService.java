package com.learnpulse.backend.service;

import com.learnpulse.backend.ai.PromptManager;
import com.learnpulse.backend.ai.StructuredAiOutputParser;
import com.learnpulse.backend.config.AiLimitsConstants;
import com.learnpulse.backend.dto.DailyScheduleDto;
import com.learnpulse.backend.dto.StudyPlanRequest;
import com.learnpulse.backend.dto.StudyPlanResponse;
import com.learnpulse.backend.dto.TaskDto;
import com.learnpulse.backend.entity.User;
import com.learnpulse.backend.exception.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudyPlanService {

    private final ChatClient chatClient;
    private final PromptManager promptManager;
    private final StructuredAiOutputParser structuredParser;

    public StudyPlanResponse generateStudyPlan(User currentUser, StudyPlanRequest request) {
        if (currentUser == null) {
            throw new ApiException("Authentication required to generate study plan.", HttpStatus.UNAUTHORIZED);
        }

        if (request == null) {
            throw new ApiException("Study plan request body is required.", HttpStatus.BAD_REQUEST);
        }

        LocalDate examDate = request.getExamDate();
        if (examDate == null) {
            throw new ApiException("Exam date is required.", HttpStatus.BAD_REQUEST);
        }

        LocalDate today = LocalDate.now();
        if (!examDate.isAfter(today)) {
            throw new ApiException("Exam date must be in the future (after today: " + today + ").", HttpStatus.BAD_REQUEST);
        }

        List<String> subjects = request.getSubjects();
        if (CollectionUtils.isEmpty(subjects)) {
            throw new ApiException("At least one subject is required for study plan generation.", HttpStatus.BAD_REQUEST);
        }

        if (subjects.size() > AiLimitsConstants.MAX_STUDY_PLAN_SUBJECTS) {
            throw new ApiException("Subjects list exceeds maximum allowed limit of " + AiLimitsConstants.MAX_STUDY_PLAN_SUBJECTS + " subjects.", HttpStatus.BAD_REQUEST);
        }

        Double availableHours = request.getAvailableHoursPerDay();
        if (availableHours == null || availableHours < 1.0 || availableHours > 16.0) {
            throw new ApiException("Available study hours per day must be between 1.0 and 16.0 hours.", HttpStatus.BAD_REQUEST);
        }

        // Deterministically calculate days remaining in backend code
        long daysRemaining = ChronoUnit.DAYS.between(today, examDate);
        log.info("Student {} generating study plan for exam date {} ({} days remaining), subjects: {}, available hours/day: {}",
                currentUser.getEmail(), examDate, daysRemaining, subjects, availableHours);

        Prompt prompt = promptManager.buildStudyPlanPrompt(examDate.toString(), daysRemaining, subjects, availableHours);
        String rawOutput;
        try {
            org.springframework.ai.chat.ChatResponse response = chatClient.call(prompt);
            rawOutput = (response != null && response.getResult() != null && response.getResult().getOutput() != null)
                    ? response.getResult().getOutput().getContent()
                    : null;
        } catch (Exception ex) {
            log.error("Ollama/ChatClient invocation failed for study planner: {}", ex.getMessage(), ex);
            throw new ApiException("The AI study planner service is currently offline or unreachable. Please try again later.", HttpStatus.SERVICE_UNAVAILABLE);
        }

        // Parse and validate raw JSON output into StudyPlanResponse DTO
        StudyPlanResponse responsePlan = structuredParser.parseAndValidate(rawOutput, StudyPlanResponse.class);

        // Populate deterministic backend calculated fields
        responsePlan.setExamDate(examDate);
        responsePlan.setDaysRemaining(daysRemaining);
        responsePlan.setAvailableHoursPerDay(availableHours);

        // Post-LLM Deterministic Business Rule Validation: Allocated daily study hours <= availableHoursPerDay
        validateStudyPlanHours(responsePlan, availableHours);

        return responsePlan;
    }

    /**
     * Deterministically validates that daily allocated task hours do not exceed available study hours per day.
     * Rejects invalid AI output cleanly without silent scaling or clamping.
     */
    private void validateStudyPlanHours(StudyPlanResponse responsePlan, double maxAvailableHoursPerDay) {
        if (responsePlan == null || CollectionUtils.isEmpty(responsePlan.getPlan())) {
            throw new ApiException("Generated study plan schedule contains no daily plan items.", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        for (DailyScheduleDto dailySchedule : responsePlan.getPlan()) {
            if (dailySchedule == null || CollectionUtils.isEmpty(dailySchedule.getTasks())) {
                continue;
            }

            double totalAllocatedHours = 0.0;
            for (TaskDto task : dailySchedule.getTasks()) {
                if (task == null || task.getHours() == null || task.getHours() < 0) {
                    throw new ApiException("Study plan contains invalid or negative task hours.", HttpStatus.INTERNAL_SERVER_ERROR);
                }
                totalAllocatedHours += task.getHours();
            }

            // Small floating point epsilon threshold (0.01)
            if (totalAllocatedHours > maxAvailableHoursPerDay + 0.01) {
                log.error("Study plan business rule violation on date {}: allocated hours ({}) exceeded available daily limit ({})",
                        dailySchedule.getDate(), totalAllocatedHours, maxAvailableHoursPerDay);
                throw new ApiException("AI generated study plan exceeded daily available study hours limit (" +
                        totalAllocatedHours + "h allocated vs " + maxAvailableHoursPerDay + "h limit on date " + dailySchedule.getDate() + ").",
                        HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
    }
}
