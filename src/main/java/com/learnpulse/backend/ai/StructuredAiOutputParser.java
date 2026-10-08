package com.learnpulse.backend.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnpulse.backend.exception.ApiException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
public class StructuredAiOutputParser {

    private final ObjectMapper objectMapper;
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    public StructuredAiOutputParser(ObjectMapper objectMapper) {
        if (objectMapper != null) {
            this.objectMapper = objectMapper.copy().findAndRegisterModules();
        } else {
            this.objectMapper = new ObjectMapper().findAndRegisterModules();
        }
    }

    /**
     * Parses raw AI LLM text response into a validated DTO instance.
     * Handles markdown code fences, extracts JSON, deserializes via Jackson, and executes Jakarta Bean Validation.
     *
     * @param rawAiOutput Raw text returned by LLM
     * @param targetClass Class of target DTO
     * @param <T> Target DTO type
     * @return Validated instance of T
     */
    public <T> T parseAndValidate(String rawAiOutput, Class<T> targetClass) {
        if (!StringUtils.hasText(rawAiOutput)) {
            log.error("AI service returned empty or null text output for target class {}", targetClass.getSimpleName());
            throw new ApiException("AI model returned an empty response. Please try again.", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // 1. Clean markdown code fences and isolate JSON text
        String jsonText = cleanJsonText(rawAiOutput);

        // 2. Deserialize JSON using Jackson ObjectMapper
        T parsedObject;
        try {
            parsedObject = objectMapper.readValue(jsonText, targetClass);
        } catch (Exception ex) {
            log.error("Failed to parse AI output into JSON for class {}. Raw output: '{}'. Error: {}",
                    targetClass.getSimpleName(), rawAiOutput, ex.getMessage());
            throw new ApiException("The AI service returned a response in an invalid format. Please try again.", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        if (parsedObject == null) {
            throw new ApiException("The AI service returned a null object structure.", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // 3. Validate DTO fields using Jakarta Bean Validation
        Set<ConstraintViolation<T>> violations = validator.validate(parsedObject);
        if (!violations.isEmpty()) {
            String errorDetails = violations.stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .collect(Collectors.joining("; "));
            log.error("Validation failed for AI generated {} object. Violations: {}", targetClass.getSimpleName(), errorDetails);
            throw new ApiException("The AI service generated output that violates validation rules: " + errorDetails, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return parsedObject;
    }

    /**
     * Extracts JSON content from raw LLM output, stripping markdown code fences and prose boundaries.
     */
    public String cleanJsonText(String rawText) {
        if (rawText == null) {
            return "";
        }

        String text = rawText.trim();

        // Strip markdown code block wrappers (e.g. ```json ... ``` or ``` ...)
        if (text.startsWith("```")) {
            int firstNewline = text.indexOf('\n');
            if (firstNewline != -1) {
                text = text.substring(firstNewline + 1);
            } else {
                text = text.substring(3);
            }
            if (text.endsWith("```")) {
                text = text.substring(0, text.length() - 3);
            }
            text = text.trim();
        }

        // Locate outer curly braces {} or square brackets [] if extra text exists
        int firstBrace = text.indexOf('{');
        int lastBrace = text.lastIndexOf('}');

        int firstBracket = text.indexOf('[');
        int lastBracket = text.lastIndexOf(']');

        if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
            if (firstBracket == -1 || firstBrace < firstBracket) {
                text = text.substring(firstBrace, lastBrace + 1);
            } else if (lastBracket > firstBracket) {
                text = text.substring(firstBracket, lastBracket + 1);
            }
        } else if (firstBracket != -1 && lastBracket != -1 && lastBracket > firstBracket) {
            text = text.substring(firstBracket, lastBracket + 1);
        }

        return text.trim();
    }
}
