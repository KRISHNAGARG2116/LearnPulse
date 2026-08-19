package com.learnpulse.backend.academic;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnpulse.backend.dto.CreateChapterRequest;
import com.learnpulse.backend.dto.CreateSubjectRequest;
import com.learnpulse.backend.entity.Role;
import com.learnpulse.backend.entity.User;
import com.learnpulse.backend.repository.ChapterRepository;
import com.learnpulse.backend.repository.SubjectRepository;
import com.learnpulse.backend.repository.UserRepository;
import com.learnpulse.backend.security.jwt.JwtProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AcademicHierarchyIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private ChapterRepository chapterRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private ObjectMapper objectMapper;

    private String adminToken;
    private String teacherToken;
    private String studentToken;

    @BeforeEach
    void setUp() {
        chapterRepository.deleteAll();
        subjectRepository.deleteAll();
        userRepository.deleteAll();

        User adminUser = userRepository.save(User.builder()
                .email("admin_acad@learnpulse.ai")
                .password(passwordEncoder.encode("AdminPass123!"))
                .role(Role.ADMIN)
                .build());
        adminToken = jwtProvider.generateAccessToken(adminUser);

        User teacherUser = userRepository.save(User.builder()
                .email("teacher_acad@learnpulse.ai")
                .password(passwordEncoder.encode("TeacherPass123!"))
                .role(Role.TEACHER)
                .build());
        teacherToken = jwtProvider.generateAccessToken(teacherUser);

        User studentUser = userRepository.save(User.builder()
                .email("student_acad@learnpulse.ai")
                .password(passwordEncoder.encode("StudentPass123!"))
                .role(Role.STUDENT)
                .build());
        studentToken = jwtProvider.generateAccessToken(studentUser);
    }

    @Test
    @DisplayName("Subject CRUD operations succeed for ADMIN and TEACHER roles")
    void testSubjectCrudOperations() throws Exception {
        // 1. Create Subject
        CreateSubjectRequest createReq = CreateSubjectRequest.builder()
                .name("Computer Science 101")
                .code("CS101")
                .description("Introduction to Computer Science Principles")
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/subjects")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("success")))
                .andExpect(jsonPath("$.data.name", is("Computer Science 101")))
                .andExpect(jsonPath("$.data.code", is("CS101")))
                .andReturn();

        String responseBody = createResult.getResponse().getContentAsString();
        String subjectIdStr = objectMapper.readTree(responseBody).path("data").path("id").asText();
        UUID subjectId = UUID.fromString(subjectIdStr);

        // 2. Get Subject by ID
        mockMvc.perform(get("/api/subjects/" + subjectId)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("success")))
                .andExpect(jsonPath("$.data.code", is("CS101")));

        // 3. List All Subjects
        mockMvc.perform(get("/api/subjects")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("success")))
                .andExpect(jsonPath("$.data", hasSize(1)));

        // 4. Update Subject (by Teacher)
        CreateSubjectRequest updateReq = CreateSubjectRequest.builder()
                .name("Advanced Computer Science 101")
                .code("CS101")
                .description("Updated CS Description")
                .build();

        mockMvc.perform(put("/api/subjects/" + subjectId)
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("success")))
                .andExpect(jsonPath("$.data.name", is("Advanced Computer Science 101")));

        // 5. Delete Subject
        mockMvc.perform(delete("/api/subjects/" + subjectId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("success")));

        assertFalse(subjectRepository.existsById(subjectId));
    }

    @Test
    @DisplayName("Chapter CRUD operations and Subject-Chapter relationship validation")
    void testChapterCrudOperations() throws Exception {
        // Create Parent Subject
        CreateSubjectRequest createSubjectReq = CreateSubjectRequest.builder()
                .name("Data Structures & Algorithms")
                .code("DSA201")
                .description("Core Data Structures")
                .build();

        MvcResult subjectRes = mockMvc.perform(post("/api/subjects")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createSubjectReq)))
                .andExpect(status().isCreated())
                .andReturn();

        String subjectIdStr = objectMapper.readTree(subjectRes.getResponse().getContentAsString()).path("data").path("id").asText();
        UUID subjectId = UUID.fromString(subjectIdStr);

        // 1. Create Chapter 1
        CreateChapterRequest createChapter1 = CreateChapterRequest.builder()
                .title("Introduction to Arrays")
                .chapterNumber(1)
                .description("Array memory layouts and indexing")
                .build();

        MvcResult chapterRes = mockMvc.perform(post("/api/subjects/" + subjectId + "/chapters")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createChapter1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("success")))
                .andExpect(jsonPath("$.data.title", is("Introduction to Arrays")))
                .andExpect(jsonPath("$.data.chapterNumber", is(1)))
                .andReturn();

        String chapterIdStr = objectMapper.readTree(chapterRes.getResponse().getContentAsString()).path("data").path("id").asText();
        UUID chapterId = UUID.fromString(chapterIdStr);

        // 2. Create Chapter 2
        CreateChapterRequest createChapter2 = CreateChapterRequest.builder()
                .title("Singly Linked Lists")
                .chapterNumber(2)
                .description("Node pointers and traversal")
                .build();

        mockMvc.perform(post("/api/subjects/" + subjectId + "/chapters")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createChapter2)))
                .andExpect(status().isCreated());

        // 3. List Chapters for Subject
        mockMvc.perform(get("/api/subjects/" + subjectId + "/chapters")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("success")))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].chapterNumber", is(1)))
                .andExpect(jsonPath("$.data[1].chapterNumber", is(2)));

        // 4. Update Chapter 1
        CreateChapterRequest updateChapter1 = CreateChapterRequest.builder()
                .title("Advanced Arrays & Vectors")
                .chapterNumber(1)
                .description("Dynamic arrays and vectors")
                .build();

        mockMvc.perform(put("/api/chapters/" + chapterId)
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateChapter1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("success")))
                .andExpect(jsonPath("$.data.title", is("Advanced Arrays & Vectors")));

        // 5. Delete Chapter 1
        mockMvc.perform(delete("/api/chapters/" + chapterId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        assertFalse(chapterRepository.existsById(chapterId));
    }

    @Test
    @DisplayName("Duplicate Subject Code returns HTTP 409 Conflict")
    void testDuplicateSubjectCodeRejected() throws Exception {
        CreateSubjectRequest subjectReq = CreateSubjectRequest.builder()
                .name("Database Systems")
                .code("DBMS301")
                .build();

        mockMvc.perform(post("/api/subjects")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(subjectReq)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/subjects")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(subjectReq)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is("error")))
                .andExpect(jsonPath("$.message", containsString("already exists")));
    }

    @Test
    @DisplayName("Creating Chapter for non-existent Subject returns HTTP 404 Not Found")
    void testChapterForNonExistentSubjectReturns404() throws Exception {
        UUID fakeSubjectId = UUID.randomUUID();
        CreateChapterRequest chapterReq = CreateChapterRequest.builder()
                .title("Orphan Chapter")
                .chapterNumber(1)
                .build();

        mockMvc.perform(post("/api/subjects/" + fakeSubjectId + "/chapters")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(chapterReq)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is("error")));
    }

    @Test
    @DisplayName("STUDENT role is forbidden from content-management modifications (HTTP 403)")
    void testStudentForbiddenFromContentModification() throws Exception {
        CreateSubjectRequest subjectReq = CreateSubjectRequest.builder()
                .name("Illegal Subject")
                .code("BAD101")
                .build();

        mockMvc.perform(post("/api/subjects")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(subjectReq)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is("error")));
    }
}
