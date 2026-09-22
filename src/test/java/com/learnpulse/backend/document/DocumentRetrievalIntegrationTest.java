package com.learnpulse.backend.document;

import com.learnpulse.backend.entity.*;
import com.learnpulse.backend.repository.*;
import com.learnpulse.backend.security.jwt.JwtProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class DocumentRetrievalIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private ChapterRepository chapterRepository;

    @Autowired
    private UploadedDocumentRepository documentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtProvider jwtProvider;

    private User student;
    private Subject subject;
    private Chapter chapter;
    private UploadedDocument doc1;
    private String studentToken;

    @BeforeEach
    void setUp() {
        documentRepository.deleteAll();
        chapterRepository.deleteAll();
        subjectRepository.deleteAll();
        userRepository.deleteAll();

        student = userRepository.save(User.builder()
                .email("student_retrieval@learnpulse.ai")
                .password(passwordEncoder.encode("StudentPass123!"))
                .role(Role.STUDENT)
                .build());
        studentToken = jwtProvider.generateAccessToken(student);

        User teacher = userRepository.save(User.builder()
                .email("teacher_retrieval@learnpulse.ai")
                .password(passwordEncoder.encode("TeacherPass123!"))
                .role(Role.TEACHER)
                .build());

        subject = subjectRepository.save(Subject.builder()
                .name("Database Systems")
                .code("DBMS301")
                .build());

        chapter = chapterRepository.save(Chapter.builder()
                .title("Relational Algebra")
                .chapterNumber(1)
                .subject(subject)
                .build());

        doc1 = documentRepository.save(UploadedDocument.builder()
                .originalFileName("relational_algebra.pdf")
                .storedFileName("stored_relational_algebra.pdf")
                .filePath("/tmp/stored_relational_algebra.pdf")
                .fileSize(1024L)
                .contentType("application/pdf")
                .teacher(teacher)
                .subject(subject)
                .chapter(chapter)
                .active(true)
                .processingStatus(ProcessingStatus.PROCESSED)
                .extractedText("Relational algebra operations...")
                .build());
    }

    @Test
    @DisplayName("GET /api/documents returns all active documents")
    void testGetAllDocuments() throws Exception {
        mockMvc.perform(get("/api/documents")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("success")))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].originalFileName", is("relational_algebra.pdf")));
    }

    @Test
    @DisplayName("GET /api/documents with subjectId and chapterId filters correctly")
    void testGetDocumentsFiltered() throws Exception {
        mockMvc.perform(get("/api/documents")
                        .param("subjectId", subject.getId().toString())
                        .param("chapterId", chapter.getId().toString())
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("success")))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].chapterTitle", is("Relational Algebra")));
    }

    @Test
    @DisplayName("GET /api/documents/{id} returns document metadata")
    void testGetDocumentById() throws Exception {
        mockMvc.perform(get("/api/documents/" + doc1.getId())
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("success")))
                .andExpect(jsonPath("$.data.id", is(doc1.getId().toString())))
                .andExpect(jsonPath("$.data.originalFileName", is("relational_algebra.pdf")));
    }
}
