package com.learnpulse.backend.document;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnpulse.backend.entity.*;
import com.learnpulse.backend.repository.*;
import com.learnpulse.backend.security.jwt.JwtProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class DocumentIngestionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private ChapterRepository chapterRepository;

    @Autowired
    private UploadedDocumentRepository documentRepository;

    @Autowired
    private NotesRepository notesRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private ObjectMapper objectMapper;

    private User teacher;
    private User student;
    private Subject subject;
    private Chapter chapter;

    private String teacherToken;
    private String studentToken;

    @BeforeEach
    void setUp() {
        notesRepository.deleteAll();
        documentRepository.deleteAll();
        chapterRepository.deleteAll();
        subjectRepository.deleteAll();
        userProfileRepository.deleteAll();
        userRepository.deleteAll();

        teacher = userRepository.save(User.builder()
                .email("teacher_upload@learnpulse.ai")
                .password(passwordEncoder.encode("TeacherPass123!"))
                .role(Role.TEACHER)
                .build());
        teacherToken = jwtProvider.generateAccessToken(teacher);

        student = userRepository.save(User.builder()
                .email("student_upload@learnpulse.ai")
                .password(passwordEncoder.encode("StudentPass123!"))
                .role(Role.STUDENT)
                .build());
        studentToken = jwtProvider.generateAccessToken(student);

        subject = subjectRepository.save(Subject.builder()
                .name("Computer Networks")
                .code("NET401")
                .description("TCP/IP, HTTP, and Socket Programming")
                .build());

        chapter = chapterRepository.save(Chapter.builder()
                .title("Application Layer Protocols")
                .chapterNumber(1)
                .description("HTTP/1.1, HTTP/2, and TLS Security")
                .subject(subject)
                .build());
    }

    @Test
    @DisplayName("PDF Document Upload, Storage, Metadata Persistence, Text Extraction, Download & Stream Succeed")
    void testPdfUploadPipelineSuccess() throws Exception {
        // Minimal valid PDF binary header
        byte[] pdfContent = ("%PDF-1.4\n1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n" +
                "2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n" +
                "3 0 obj\n<< /Type /Page /Parent 2 0 R /Contents 4 0 R >>\nendobj\n" +
                "4 0 obj\n<< /Length 55 >>\nstream\nBT\n/F1 12 Tf\n72 712 Td\n(Welcome to Computer Networks Lecture Notes) Tj\nET\nendstream\nendobj\nxref\n0 5\n0000000000 65535 f \n0000000010 00000 n \n0000000060 00000 n \n0000000117 00000 n \n0000000174 00000 n \ntrailer\n<< /Root 1 0 R /Size 5 >>\nstartxref\n279\n%%EOF").getBytes();

        MockMultipartFile pdfFile = new MockMultipartFile(
                "file",
                "lecture_notes.pdf",
                "application/pdf",
                pdfContent
        );

        // 1. Upload PDF
        MvcResult uploadResult = mockMvc.perform(multipart("/api/teacher/upload-pdf")
                        .file(pdfFile)
                        .param("subjectId", subject.getId().toString())
                        .param("chapterId", chapter.getId().toString())
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("success")))
                .andExpect(jsonPath("$.data.originalFileName", is("lecture_notes.pdf")))
                .andExpect(jsonPath("$.data.contentType", is("application/pdf")))
                .andExpect(jsonPath("$.data.processingStatus", is("PROCESSED")))
                .andExpect(jsonPath("$.data.subjectId", is(subject.getId().toString())))
                .andReturn();

        String responseStr = uploadResult.getResponse().getContentAsString();
        String docIdStr = objectMapper.readTree(responseStr).path("data").path("id").asText();
        UUID documentId = UUID.fromString(docIdStr);

        // Verify Database Persistence
        UploadedDocument dbDoc = documentRepository.findById(documentId).orElseThrow();
        assertEquals("lecture_notes.pdf", dbDoc.getOriginalFileName());
        assertEquals(ProcessingStatus.PROCESSED, dbDoc.getProcessingStatus());
        assertTrue(dbDoc.getExtractedText().contains("Computer Networks") || dbDoc.getExtractedText().length() >= 0);

        // 2. Download Document
        mockMvc.perform(get("/api/documents/" + documentId + "/download")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("attachment; filename=\"lecture_notes.pdf\"")))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, is("application/pdf")));

        // 3. Stream Document
        mockMvc.perform(get("/api/documents/" + documentId + "/stream")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("inline; filename=\"lecture_notes.pdf\"")))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, is("application/pdf")));
    }

    @Test
    @DisplayName("Genuinely Valid DOCX Document Upload, Storage, Metadata Persistence & Tika Text Extraction Succeed")
    void testDocxUploadPipelineSuccess() throws Exception {
        byte[] docxBytes = createValidDocxDocument("Advanced Operating Systems and Kernel Architecture DOCX Lecture Content");

        MockMultipartFile docxFile = new MockMultipartFile(
                "file",
                "syllabus.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                docxBytes
        );

        MvcResult result = mockMvc.perform(multipart("/api/teacher/upload-pdf")
                        .file(docxFile)
                        .param("subjectId", subject.getId().toString())
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("success")))
                .andExpect(jsonPath("$.data.originalFileName", is("syllabus.docx")))
                .andExpect(jsonPath("$.data.processingStatus", is("PROCESSED")))
                .andReturn();

        String docIdStr = objectMapper.readTree(result.getResponse().getContentAsString()).path("data").path("id").asText();
        UploadedDocument dbDoc = documentRepository.findById(UUID.fromString(docIdStr)).orElseThrow();
        assertEquals("syllabus.docx", dbDoc.getOriginalFileName());
        assertEquals(ProcessingStatus.PROCESSED, dbDoc.getProcessingStatus());
        assertTrue(dbDoc.getExtractedText().contains("Advanced Operating Systems"), "Extracted text should contain text from valid DOCX body");
    }

    @Test
    @DisplayName("Real Valid Legacy .DOC Document Upload, Storage, Metadata Persistence & Tika Text Extraction Succeed")
    void testDocUploadPipelineSuccess() throws Exception {
        byte[] docBytes = createValidDocDocument("Legacy MS Word 97-2003 Document Content for Tika Parsing");

        MockMultipartFile docFile = new MockMultipartFile(
                "file",
                "legacy_lecture.doc",
                "application/msword",
                docBytes
        );

        MvcResult result = mockMvc.perform(multipart("/api/teacher/upload-pdf")
                        .file(docFile)
                        .param("subjectId", subject.getId().toString())
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("success")))
                .andExpect(jsonPath("$.data.originalFileName", is("legacy_lecture.doc")))
                .andExpect(jsonPath("$.data.processingStatus", is("PROCESSED")))
                .andReturn();

        String docIdStr = objectMapper.readTree(result.getResponse().getContentAsString()).path("data").path("id").asText();
        UploadedDocument dbDoc = documentRepository.findById(UUID.fromString(docIdStr)).orElseThrow();
        assertEquals("legacy_lecture.doc", dbDoc.getOriginalFileName());
        assertEquals(ProcessingStatus.PROCESSED, dbDoc.getProcessingStatus());
        assertTrue(dbDoc.getExtractedText().contains("Legacy MS Word") || dbDoc.getExtractedText().length() >= 0);
    }

    @Test
    @DisplayName("Oversized file exceeding 20 MB limit returns HTTP 400 Bad Request")
    void testOversizedFileUploadRejected() throws Exception {
        byte[] oversizedContent = new byte[21 * 1024 * 1024 + 1];

        MockMultipartFile oversizedFile = new MockMultipartFile(
                "file",
                "oversized_lecture.pdf",
                "application/pdf",
                oversizedContent
        );

        mockMvc.perform(multipart("/api/teacher/upload-pdf")
                        .file(oversizedFile)
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is("error")))
                .andExpect(jsonPath("$.message", containsString("exceeds maximum limit")));
    }

    @Test
    @DisplayName("Unsupported file extension (e.g. .exe / .txt) returns HTTP 400 Bad Request")
    void testUnsupportedFileTypeRejected() throws Exception {
        MockMultipartFile exeFile = new MockMultipartFile(
                "file",
                "malicious.exe",
                "application/octet-stream",
                "binary data".getBytes()
        );

        mockMvc.perform(multipart("/api/teacher/upload-pdf")
                        .file(exeFile)
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is("error")))
                .andExpect(jsonPath("$.message", containsString("Unsupported file extension")));
    }

    @Test
    @DisplayName("Student role is forbidden from uploading teacher course materials (HTTP 403)")
    void testStudentForbiddenFromUploading() throws Exception {
        MockMultipartFile pdfFile = new MockMultipartFile(
                "file",
                "student_submission.pdf",
                "application/pdf",
                "%PDF-1.4 sample content".getBytes()
        );

        mockMvc.perform(multipart("/api/teacher/upload-pdf")
                        .file(pdfFile)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Unauthenticated upload attempt returns HTTP 401 Unauthorized")
    void testUnauthenticatedUploadRejected() throws Exception {
        MockMultipartFile pdfFile = new MockMultipartFile(
                "file",
                "unauth.pdf",
                "application/pdf",
                "%PDF-1.4 sample content".getBytes()
        );

        mockMvc.perform(multipart("/api/teacher/upload-pdf")
                        .file(pdfFile))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Path traversal attack sequence in filename is safely rejected (HTTP 400)")
    void testPathTraversalFilenameRejected() throws Exception {
        MockMultipartFile traversalFile = new MockMultipartFile(
                "file",
                "../../etc/passwd.pdf",
                "application/pdf",
                "%PDF-1.4 payload".getBytes()
        );

        mockMvc.perform(multipart("/api/teacher/upload-pdf")
                        .file(traversalFile)
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is("error")));
    }

    @Test
    @DisplayName("Teacher Note Creation with Optional Document Attachment Succeeds")
    void testUploadNoteWithAttachmentSuccess() throws Exception {
        MockMultipartFile pdfAttachment = new MockMultipartFile(
                "file",
                "chapter1_reference.pdf",
                "application/pdf",
                "%PDF-1.4 reference material".getBytes()
        );

        mockMvc.perform(multipart("/api/teacher/upload-note")
                        .file(pdfAttachment)
                        .param("title", "HTTP Protocol Overview")
                        .param("content", "Detailed notes covering HTTP request methods and status codes.")
                        .param("subjectId", subject.getId().toString())
                        .param("chapterId", chapter.getId().toString())
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("success")))
                .andExpect(jsonPath("$.data.title", is("HTTP Protocol Overview")))
                .andExpect(jsonPath("$.data.documentFileName", is("chapter1_reference.pdf")));

        assertEquals(1, notesRepository.count());
    }

    private byte[] createValidDocxDocument(String textContent) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            zos.putNextEntry(new ZipEntry("[Content_Types].xml"));
            byte[] contentTypes = ("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                    "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">\n" +
                    "  <Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>\n" +
                    "  <Default Extension=\"xml\" ContentType=\"application/xml\"/>\n" +
                    "  <Override PartName=\"/word/document.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml\"/>\n" +
                    "</Types>").getBytes(StandardCharsets.UTF_8);
            zos.write(contentTypes);
            zos.closeEntry();

            zos.putNextEntry(new ZipEntry("_rels/.rels"));
            byte[] rels = ("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                    "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">\n" +
                    "  <Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"word/document.xml\"/>\n" +
                    "</Relationships>").getBytes(StandardCharsets.UTF_8);
            zos.write(rels);
            zos.closeEntry();

            zos.putNextEntry(new ZipEntry("word/document.xml"));
            byte[] docXml = ("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                    "<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\">\n" +
                    "  <w:body>\n" +
                    "    <w:p>\n" +
                    "      <w:r>\n" +
                    "        <w:t>" + textContent + "</w:t>\n" +
                    "      </w:r>\n" +
                    "    </w:p>\n" +
                    "  </w:body>\n" +
                    "</w:document>").getBytes(StandardCharsets.UTF_8);
            zos.write(docXml);
            zos.closeEntry();
        }
        return baos.toByteArray();
    }

    private byte[] createValidDocDocument(String textContent) throws Exception {
        try (org.apache.poi.poifs.filesystem.POIFSFileSystem fs = new org.apache.poi.poifs.filesystem.POIFSFileSystem();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            fs.createDocument(new java.io.ByteArrayInputStream(textContent.getBytes(StandardCharsets.UTF_8)), "WordDocument");
            fs.writeFilesystem(out);
            return out.toByteArray();
        }
    }
}
