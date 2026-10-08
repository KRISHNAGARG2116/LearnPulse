package com.learnpulse.backend.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnpulse.backend.dto.SummaryResponse;
import com.learnpulse.backend.exception.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StructuredOutputParserTest {

    private StructuredAiOutputParser parser;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        parser = new StructuredAiOutputParser(objectMapper);
    }

    @Test
    @DisplayName("Should strip markdown code fences and parse valid JSON")
    void testCleanJsonTextAndParseValidObject() {
        String rawMarkdownJson = """
                ```json
                {
                  "summary": "This is a clean summary.",
                  "keyTakeaways": ["Point 1", "Point 2"],
                  "keywords": ["Java", "OOP"]
                }
                ```
                """;

        SummaryResponse response = parser.parseAndValidate(rawMarkdownJson, SummaryResponse.class);
        assertNotNull(response);
        assertEquals("This is a clean summary.", response.getSummary());
        assertEquals(2, response.getKeyTakeaways().size());
        assertEquals(2, response.getKeywords().size());
    }

    @Test
    @DisplayName("Should parse JSON surrounded by prose commentary")
    void testParseJsonWithSurroundingProse() {
        String rawOutput = """
                Here is your requested JSON response:
                {
                  "summary": "Summary text here.",
                  "keyTakeaways": ["Takeaway 1"],
                  "keywords": ["Keyword1"]
                }
                Hope this helps!
                """;

        SummaryResponse response = parser.parseAndValidate(rawOutput, SummaryResponse.class);
        assertNotNull(response);
        assertEquals("Summary text here.", response.getSummary());
    }

    @Test
    @DisplayName("Should throw ApiException when JSON is invalid or malformed")
    void testParseInvalidJsonThrowsApiException() {
        String malformedJson = "{ summary: 'broken json without quotes' ";
        assertThrows(ApiException.class, () -> parser.parseAndValidate(malformedJson, SummaryResponse.class));
    }

    @Test
    @DisplayName("Should throw ApiException when required DTO fields are missing or empty")
    void testValidationFailureThrowsApiException() {
        String missingFieldsJson = """
                {
                  "summary": "",
                  "keyTakeaways": [],
                  "keywords": []
                }
                """;

        assertThrows(ApiException.class, () -> parser.parseAndValidate(missingFieldsJson, SummaryResponse.class));
    }

    @Test
    @DisplayName("Should throw ApiException when raw output is null or blank")
    void testNullOrBlankOutputThrowsApiException() {
        assertThrows(ApiException.class, () -> parser.parseAndValidate(null, SummaryResponse.class));
        assertThrows(ApiException.class, () -> parser.parseAndValidate("   ", SummaryResponse.class));
    }
}
