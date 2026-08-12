package com.learnpulse.backend.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class BaseStatusControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Verify /api/v1/status returns 200 OK with standardized success ApiResponse structure")
    void testGetBaseStatusSuccess() throws Exception {
        mockMvc.perform(get("/api/v1/status")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("success")))
                .andExpect(jsonPath("$.message", is("Backend infrastructure operating successfully")))
                .andExpect(jsonPath("$.timestamp", notNullValue()))
                .andExpect(jsonPath("$.data.application", is("learning-assistant")))
                .andExpect(jsonPath("$.data.environmentStatus", is("UP")))
                .andExpect(jsonPath("$.data.database.databaseName", is("learning_assistant_db")))
                .andExpect(jsonPath("$.data.database.connected", is(true)))
                .andExpect(jsonPath("$.data.database.pgvectorInstalled", is(true)))
                .andExpect(jsonPath("$.errors", nullValue()));
    }
}
