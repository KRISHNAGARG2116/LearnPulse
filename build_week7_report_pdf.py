import os
import sys
from reportlab.lib.pagesizes import letter
from reportlab.lib import colors
from reportlab.platypus import (
    SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, PageBreak, KeepTogether, HRFlowable
)
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib.units import inch
from reportlab.pdfgen import canvas

class NumberedCanvas(canvas.Canvas):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self._saved_page_states = []

    def showPage(self):
        self._saved_page_states.append(dict(self.__dict__))
        self._startPage()

    def save(self):
        num_pages = len(self._saved_page_states)
        for state in self._saved_page_states:
            self.__dict__.update(state)
            self.draw_page_decorations(num_pages)
            super().showPage()
        super().save()

    def draw_page_decorations(self, page_count):
        self.saveState()
        self.setFont("Helvetica-Bold", 8)
        self.setFillColor(colors.HexColor("#4A5568"))
        
        # Header (pages 2+)
        if self._pageNumber > 1:
            self.drawString(54, 11 * inch - 36, "LEARNPULSE LMS — WEEK 7 IMPLEMENTATION REPORT")
            self.drawRightString(8.5 * inch - 54, 11 * inch - 36, "COURSE PROGRESSION & REACT FRONTEND")
            self.setStrokeColor(colors.HexColor("#CBD5E0"))
            self.setLineWidth(0.75)
            self.line(54, 11 * inch - 42, 8.5 * inch - 54, 11 * inch - 42)
        
        # Footer
        self.setStrokeColor(colors.HexColor("#CBD5E0"))
        self.setLineWidth(0.75)
        self.line(54, 48, 8.5 * inch - 54, 48)
        
        self.setFont("Helvetica", 8)
        self.drawString(54, 32, "CONFIDENTIAL & PROPRIETARY — LEVERIFY / LEARNPULSE AI")
        page_str = f"Page {self._pageNumber} of {page_count}"
        self.drawRightString(8.5 * inch - 54, 32, page_str)
        self.restoreState()

def build_pdf(filename="WEEK_7_IMPLEMENTATION_REPORT.pdf"):
    doc = SimpleDocTemplate(
        filename,
        pagesize=letter,
        leftMargin=54,
        rightMargin=54,
        topMargin=54,
        bottomMargin=54
    )

    styles = getSampleStyleSheet()

    # Custom styles
    title_style = ParagraphStyle(
        'CoverTitle',
        parent=styles['Normal'],
        fontName='Helvetica-Bold',
        fontSize=24,
        leading=28,
        textColor=colors.HexColor("#1A365D"),
        alignment=0,
        spaceAfter=10
    )

    subtitle_style = ParagraphStyle(
        'CoverSubtitle',
        parent=styles['Normal'],
        fontName='Helvetica',
        fontSize=13,
        leading=16,
        textColor=colors.HexColor("#2B6CB0"),
        alignment=0,
        spaceAfter=20
    )

    h1_style = ParagraphStyle(
        'SectionH1',
        parent=styles['Normal'],
        fontName='Helvetica-Bold',
        fontSize=14,
        leading=18,
        textColor=colors.HexColor("#1A365D"),
        spaceBefore=14,
        spaceAfter=8,
        keepWithNext=True
    )

    h2_style = ParagraphStyle(
        'SectionH2',
        parent=styles['Normal'],
        fontName='Helvetica-Bold',
        fontSize=11,
        leading=14,
        textColor=colors.HexColor("#2D3748"),
        spaceBefore=10,
        spaceAfter=6,
        keepWithNext=True
    )

    body_style = ParagraphStyle(
        'BodyDark',
        parent=styles['Normal'],
        fontName='Helvetica',
        fontSize=9.5,
        leading=13.5,
        textColor=colors.HexColor("#2D3748"),
        spaceAfter=6
    )

    code_style = ParagraphStyle(
        'CodeSnippet',
        parent=styles['Normal'],
        fontName='Courier',
        fontSize=8,
        leading=10.5,
        textColor=colors.HexColor("#1A202C"),
        backColor=colors.HexColor("#F7FAFC"),
        borderColor=colors.HexColor("#E2E8F0"),
        borderWidth=0.5,
        borderPadding=6,
        spaceBefore=4,
        spaceAfter=6
    )

    table_header_style = ParagraphStyle(
        'TableHeader',
        parent=styles['Normal'],
        fontName='Helvetica-Bold',
        fontSize=8.5,
        leading=11,
        textColor=colors.white,
        alignment=0
    )

    table_cell_style = ParagraphStyle(
        'TableCell',
        parent=styles['Normal'],
        fontName='Helvetica',
        fontSize=8.5,
        leading=11,
        textColor=colors.HexColor("#2D3748"),
        alignment=0
    )

    story = []

    # Title Banner
    story.append(Paragraph("WEEK 7 IMPLEMENTATION REPORT", title_style))
    story.append(Paragraph("Course Progression Architecture & Foundational React 19 SPA Setup", subtitle_style))
    story.append(HRFlowable(width="100%", thickness=2, color=colors.HexColor("#3182CE"), spaceAfter=15))

    # Metadata Block
    meta_data = [
        [Paragraph("<b>Project Phase:</b>", table_cell_style), Paragraph("Week 7 Implementation", table_cell_style), Paragraph("<b>Status:</b>", table_cell_style), Paragraph("<font color='#2F855A'><b>COMPLETED (80/80 Tests Passed)</b></font>", table_cell_style)],
        [Paragraph("<b>Author:</b>", table_cell_style), Paragraph("Engineering Architecture Team", table_cell_style), Paragraph("<b>Date:</b>", table_cell_style), Paragraph("September 16, 2026", table_cell_style)],
        [Paragraph("<b>Technology Stack:</b>", table_cell_style), Paragraph("Spring Boot 3.2, React 19, TypeScript, Vite, Tailwind v4", table_cell_style), Paragraph("<b>Test Suite:</b>", table_cell_style), Paragraph("80 Backend Tests + Vite Build 0 Errors", table_cell_style)]
    ]
    meta_table = Table(meta_data, colWidths=[1.3*inch, 2.2*inch, 1.2*inch, 2.3*inch])
    meta_table.setStyle(TableStyle([
        ('BACKGROUND', (0,0), (-1,-1), colors.HexColor("#EDF2F7")),
        ('PADDING', (0,0), (-1,-1), 6),
        ('GRID', (0,0), (-1,-1), 0.5, colors.HexColor("#CBD5E0")),
        ('VALIGN', (0,0), (-1,-1), 'MIDDLE'),
    ]))
    story.append(meta_table)
    story.append(Spacer(1, 15))

    # Executive Summary
    story.append(Paragraph("1. Executive Summary & Objectives", h1_style))
    exec_summary_text = (
        "Week 7 accomplishes two major milestones for LearnPulse LMS: extending the backend domain model to support "
        "mastery-based <b>Course Progression Architecture</b> (Course -> Chapters -> Chapter Quizzes -> Progression -> Final Quiz) "
        "and establishing the foundational <b>React 19 Single Page Application (SPA)</b> infrastructure with TypeScript, Vite, "
        "Tailwind CSS v4, Axios, React Router v7, and React Hook Form. "
        "Server-side progression rules guarantee that Chapter 1 is unlocked by default, passing Chapter N quiz with >= 80% score unlocks Chapter N+1, "
        "passing <b>ALL chapters belonging to the Subject/Course</b> unlocks the Final Course Quiz, and scoring >= 75% on the Final Quiz marks the course completed."
    )
    story.append(Paragraph(exec_summary_text, body_style))
    story.append(Spacer(1, 10))

    # Architecture Overview
    story.append(Paragraph("2. Progression Architecture Hierarchy", h1_style))
    story.append(Paragraph("The domain extends the existing Subject entity container without creating duplicate structures (diagram shows an illustrative N-chapter flow):", body_style))
    
    arch_code = (
        "Subject (Course Container: e.g. JAVA101)\n"
        "  ├── Chapter 1: Basics -> Chapter Quiz 1 (Passing: 80%) [UNLOCKED BY DEFAULT]\n"
        "  ├── Chapter 2..N: Concepts -> Chapter Quiz N (Passing: 80%) [LOCKED until N-1 Passed]\n"
        "  └── Final Course Assessment (Passing: 75%) [LOCKED until ALL Chapters Passed]\n"
        "        └── Status: COMPLETED upon scoring >= 75%"
    )
    story.append(Paragraph(arch_code.replace("\n", "<br/>"), code_style))
    story.append(Spacer(1, 10))

    # Domain Data Model Extensions
    story.append(Paragraph("3. Extended Domain Enums & Entities", h1_style))
    schema_data = [
        [Paragraph("Component", table_header_style), Paragraph("Type / Target", table_header_style), Paragraph("Details & Values", table_header_style), Paragraph("Purpose / Constraints", table_header_style)],
        [Paragraph("QuizType", table_cell_style), Paragraph("Enum", table_cell_style), Paragraph("CHAPTER_QUIZ, FINAL_COURSE_QUIZ", table_cell_style), Paragraph("Discriminator for progression rules", table_cell_style)],
        [Paragraph("QuizStatus", table_cell_style), Paragraph("Enum", table_cell_style), Paragraph("DRAFT, PUBLISHED", table_cell_style), Paragraph("Teacher publishing workflow control", table_cell_style)],
        [Paragraph("QuestionSource", table_cell_style), Paragraph("Enum", table_cell_style), Paragraph("TEACHER_MANUAL, TEACHER_UPLOADED, AI_GENERATED", table_cell_style), Paragraph("Supported question source architecture", table_cell_style)],
        [Paragraph("Quiz Entity", table_cell_style), Paragraph("JPA Entity", table_cell_style), Paragraph("passingScorePercentage (80.0 / 75.0), status, quizType", table_cell_style), Paragraph("Linked to Subject and Chapter", table_cell_style)],
        [Paragraph("Question Entity", table_cell_style), Paragraph("JPA Entity", table_cell_style), Paragraph("teacherPriority, customInstruction, source", table_cell_style), Paragraph("Extension metadata for question sources", table_cell_style)]
    ]
    schema_table = Table(schema_data, colWidths=[1.3*inch, 1.1*inch, 2.4*inch, 2.2*inch])
    schema_table.setStyle(TableStyle([
        ('BACKGROUND', (0,0), (-1,0), colors.HexColor("#1A365D")),
        ('PADDING', (0,0), (-1,-1), 5),
        ('GRID', (0,0), (-1,-1), 0.5, colors.HexColor("#CBD5E0")),
        ('VALIGN', (0,0), (-1,-1), 'MIDDLE'),
        ('ROWBACKGROUNDS', (0,1), (-1,-1), [colors.white, colors.HexColor("#F7FAFC")]),
    ]))
    story.append(schema_table)
    story.append(Spacer(1, 12))

    # React Frontend Infrastructure
    story.append(Paragraph("4. React 19 Frontend SPA Infrastructure", h1_style))
    fe_data = [
        [Paragraph("Layer / Module", table_header_style), Paragraph("Technology / File", table_header_style), Paragraph("Implementation Details & Features", table_header_style)],
        [Paragraph("HTTP Client & Auth Interceptor", table_cell_style), Paragraph("src/api/axios.ts", table_cell_style), Paragraph("Axios instance with automatic Bearer token injection & 401 refresh token handler", table_cell_style)],
        [Paragraph("Authentication State Context", table_cell_style), Paragraph("src/context/AuthContext.tsx", table_cell_style), Paragraph("Global AuthProvider storing user state, login, register, logout, and token persistence", table_cell_style)],
        [Paragraph("Protected Route Guard", table_cell_style), Paragraph("src/components/ProtectedRoute.tsx", table_cell_style), Paragraph("RBAC authorization wrapper protecting STUDENT, TEACHER, and ADMIN routes", table_cell_style)],
        [Paragraph("Registration Security", table_cell_style), Paragraph("src/pages/auth/RegisterPage.tsx", table_cell_style), Paragraph("Public registration restricted strictly to STUDENT and TEACHER roles (ADMIN excluded)", table_cell_style)],
        [Paragraph("Application Pages", table_cell_style), Paragraph("src/pages/*", table_cell_style), Paragraph("Built Login, Register, Student Dashboard/Practice/Results/Profile, Teacher Dashboard/Questions, Admin Dashboard/Users", table_cell_style)]
    ]
    fe_table = Table(fe_data, colWidths=[1.8*inch, 1.8*inch, 3.4*inch])
    fe_table.setStyle(TableStyle([
        ('BACKGROUND', (0,0), (-1,0), colors.HexColor("#2B6CB0")),
        ('PADDING', (0,0), (-1,-1), 5),
        ('GRID', (0,0), (-1,-1), 0.5, colors.HexColor("#CBD5E0")),
        ('VALIGN', (0,0), (-1,-1), 'MIDDLE'),
        ('ROWBACKGROUNDS', (0,1), (-1,-1), [colors.white, colors.HexColor("#F7FAFC")]),
    ]))
    story.append(fe_table)
    story.append(Spacer(1, 12))

    # Test Results & Verification
    story.append(Paragraph("5. Automated Testing & Verification Results", h1_style))
    test_summary = (
        "The complete backend Maven integration test suite runs <b>80 automated integration tests</b> with <b>0 failures</b>. "
        "Frontend TypeScript compilation (<code>tsc -b</code>) and Vite production bundling (<code>vite build</code>) completed with <b>0 errors</b>."
    )
    story.append(Paragraph(test_summary, body_style))

    test_data = [
        [Paragraph("Test Class Name", table_header_style), Paragraph("Tests", table_header_style), Paragraph("Status", table_header_style), Paragraph("Coverage Area", table_header_style)],
        [Paragraph("CourseProgressionIntegrationTest", table_cell_style), Paragraph("10", table_cell_style), Paragraph("<font color='#2F855A'><b>10 PASSED</b></font>", table_cell_style), Paragraph("Single & multi-chapter progression locking/unlocking", table_cell_style)],
        [Paragraph("AssessmentEngineIntegrationTest", table_cell_style), Paragraph("39", table_cell_style), Paragraph("<font color='#2F855A'><b>39 PASSED</b></font>", table_cell_style), Paragraph("Quiz delivery, answer reveals, server grading", table_cell_style)],
        [Paragraph("SecurityRbacIntegrationTest", table_cell_style), Paragraph("7", table_cell_style), Paragraph("<font color='#2F855A'><b>7 PASSED</b></font>", table_cell_style), Paragraph("JWT authentication, RBAC authorization, registration", table_cell_style)],
        [Paragraph("DocumentIngestionIntegrationTest", table_cell_style), Paragraph("9", table_cell_style), Paragraph("<font color='#2F855A'><b>9 PASSED</b></font>", table_cell_style), Paragraph("PDF/DOC/DOCX upload & Tika extraction", table_cell_style)],
        [Paragraph("StudentTeacherProfile & Academic", table_cell_style), Paragraph("10", table_cell_style), Paragraph("<font color='#2F855A'><b>10 PASSED</b></font>", table_cell_style), Paragraph("Profiles, Subject & Chapter hierarchy", table_cell_style)],
        [Paragraph("JwtProvider & Application Context", table_cell_style), Paragraph("5", table_cell_style), Paragraph("<font color='#2F855A'><b>5 PASSED</b></font>", table_cell_style), Paragraph("Token generation, status endpoints, app context", table_cell_style)],
        [Paragraph("<b>TOTAL PROJECT SUITE</b>", table_cell_style), Paragraph("<b>80</b>", table_cell_style), Paragraph("<font color='#2F855A'><b>80 PASSED</b></font>", table_cell_style), Paragraph("<b>BUILD SUCCESS (Frontend Build 0 Errors)</b>", table_cell_style)]
    ]
    test_table = Table(test_data, colWidths=[2.5*inch, 0.7*inch, 1.4*inch, 2.4*inch])
    test_table.setStyle(TableStyle([
        ('BACKGROUND', (0,0), (-1,0), colors.HexColor("#2D3748")),
        ('PADDING', (0,0), (-1,-1), 5),
        ('GRID', (0,0), (-1,-1), 0.5, colors.HexColor("#CBD5E0")),
        ('VALIGN', (0,0), (-1,-1), 'MIDDLE'),
        ('ROWBACKGROUNDS', (0,1), (-1,-2), [colors.white, colors.HexColor("#F7FAFC")]),
        ('BACKGROUND', (0,-1), (-1,-1), colors.HexColor("#E2E8F0")),
    ]))
    story.append(test_table)
    story.append(Spacer(1, 15))

    # Sign-off Block
    story.append(Paragraph("6. Verification & Mentor Sign-Off", h1_style))
    signoff_text = (
        "<b>Verification Confirmed:</b> All Week 7 Course Progression Architecture extensions and foundational React 19 Frontend SPA "
        "components have been fully verified with 80 passing backend tests and a clean production Vite build. "
        "Final Course Quiz unlocks only after ALL chapters have been passed, QuestionSource supports TEACHER_MANUAL, TEACHER_UPLOADED, and AI_GENERATED, "
        "and public user registration excludes ADMIN privileges."
    )
    story.append(Paragraph(signoff_text, body_style))

    doc.build(story, canvasmaker=NumberedCanvas)
    print(f"Report PDF Successfully Generated: {os.path.abspath(filename)}")

if __name__ == '__main__':
    build_pdf()
