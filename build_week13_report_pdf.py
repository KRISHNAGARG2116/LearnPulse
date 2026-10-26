import os
import sys
from reportlab.lib.pagesizes import letter
from reportlab.lib import colors
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.platypus import (
    SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, HRFlowable, KeepTogether
)
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
        self.setFont("Helvetica", 8)
        self.setFillColor(colors.HexColor("#64748B"))
        
        # Header (pages > 1)
        if self._pageNumber > 1:
            self.drawString(40, 755, "LearnPulse AI LMS — Week 13 Implementation & Compliance Remediation Report")
            self.setStrokeColor(colors.HexColor("#E2E8F0"))
            self.setLineWidth(0.5)
            self.line(40, 748, 572, 748)
            
        # Footer
        footer_text = f"Page {self._pageNumber} of {page_count}"
        self.drawRightString(572, 28, footer_text)
        self.drawString(40, 28, "CONFIDENTIAL & PROPRIETARY — LEARNPULSE AI LMS (OFFICIAL WEEK 13)")
        self.setStrokeColor(colors.HexColor("#E2E8F0"))
        self.setLineWidth(0.5)
        self.line(40, 38, 572, 38)
        
        self.restoreState()

def build_pdf(filename="WEEK_13_IMPLEMENTATION_REPORT.pdf"):
    doc = SimpleDocTemplate(
        filename,
        pagesize=letter,
        leftMargin=40,
        rightMargin=40,
        topMargin=42,
        bottomMargin=42
    )
    
    styles = getSampleStyleSheet()
    
    # Custom Palette
    primary = colors.HexColor("#0F172A")   # Slate 900
    secondary = colors.HexColor("#2563EB") # Blue 600
    dark_text = colors.HexColor("#1E293B") # Slate 800
    light_bg = colors.HexColor("#F8FAFC")  # Slate Light
    alt_bg = colors.HexColor("#F1F5F9")
    border_col = colors.HexColor("#CBD5E1")
    accent_green = colors.HexColor("#059669")
    accent_blue = colors.HexColor("#0284C7")
    accent_amber = colors.HexColor("#D97706")
    accent_purple = colors.HexColor("#7C3AED")

    title_style = ParagraphStyle(
        'DocTitle',
        parent=styles['Title'],
        fontName='Helvetica-Bold',
        fontSize=14,
        leading=17,
        textColor=primary,
        alignment=0,
        spaceAfter=2
    )

    subtitle_style = ParagraphStyle(
        'DocSubTitle',
        parent=styles['Normal'],
        fontName='Helvetica-Bold',
        fontSize=9,
        leading=11.5,
        textColor=secondary,
        spaceAfter=5
    )

    h1_style = ParagraphStyle(
        'Heading1_Custom',
        parent=styles['Heading1'],
        fontName='Helvetica-Bold',
        fontSize=9.5,
        leading=12,
        textColor=primary,
        spaceBefore=5,
        spaceAfter=3,
        keepWithNext=True
    )

    h2_style = ParagraphStyle(
        'Heading2_Custom',
        parent=styles['Heading2'],
        fontName='Helvetica-Bold',
        fontSize=8.5,
        leading=11,
        textColor=secondary,
        spaceBefore=4,
        spaceAfter=2,
        keepWithNext=True
    )

    body_style = ParagraphStyle(
        'Body_Custom',
        parent=styles['Normal'],
        fontName='Helvetica',
        fontSize=7.2,
        leading=9.2,
        textColor=dark_text,
        spaceAfter=3
    )

    bullet_style = ParagraphStyle(
        'Bullet_Custom',
        parent=styles['Normal'],
        fontName='Helvetica',
        fontSize=7.0,
        leading=9.0,
        textColor=dark_text,
        leftIndent=8,
        firstLineIndent=-8,
        spaceAfter=1.5
    )

    table_header_style = ParagraphStyle(
        'TableHeader',
        parent=body_style,
        fontName='Helvetica-Bold',
        fontSize=7.0,
        leading=8.5,
        textColor=colors.white,
        alignment=0
    )

    table_cell_style = ParagraphStyle(
        'TableCell',
        parent=body_style,
        fontName='Helvetica',
        fontSize=6.5,
        leading=8.0,
        spaceAfter=0
    )

    table_cell_bold = ParagraphStyle(
        'TableCellBold',
        parent=body_style,
        fontName='Helvetica-Bold',
        fontSize=6.5,
        leading=8.0,
        spaceAfter=0
    )

    meta_label = ParagraphStyle(
        'MetaLabel',
        parent=body_style,
        fontName='Helvetica-Bold',
        fontSize=7.2,
        leading=9.0,
        textColor=colors.HexColor("#475569")
    )

    story = []

    # Title & Metadata Header Block
    story.append(Paragraph("LearnPulse — Week 13 Implementation & Compliance Remediation Report", title_style))
    story.append(Paragraph("Administrative Dashboard, User Governance & System Analytics (Days 73–78)", subtitle_style))
    story.append(HRFlowable(width="100%", thickness=1.5, color=secondary, spaceAfter=4, spaceBefore=0))

    meta_data = [
        [
            Paragraph("<b>Document Version:</b> 2.0.0 (Remediated)", meta_label),
            Paragraph("<b>Milestone:</b> Official Week 13 Implementation", meta_label),
            Paragraph("<b>Target Platform:</b> Spring Boot 3.2.5 + React 19", meta_label)
        ],
        [
            Paragraph("<b>Compliance Status:</b> FULLY COMPLIANT & VERIFIED", meta_label),
            Paragraph("<b>Automated Tests:</b> 235 / 235 PASSED (100%)", meta_label),
            Paragraph("<b>Vite Frontend:</b> 1,697 modules transformed (0 errors)", meta_label)
        ],
        [
            Paragraph("<b>Audit Remediations:</b> P1–P7 COMPLETE", meta_label),
            Paragraph("<b>Date:</b> October 27, 2026", meta_label),
            Paragraph("<b>Confidentiality:</b> STRICTLY CONFIDENTIAL", meta_label)
        ]
    ]
    t_meta = Table(meta_data, colWidths=[180, 180, 172])
    t_meta.setStyle(TableStyle([
        ('BACKGROUND', (0,0), (-1,-1), light_bg),
        ('BOX', (0,0), (-1,-1), 0.5, border_col),
        ('VALIGN', (0,0), (-1,-1), 'MIDDLE'),
        ('TOPPADDING', (0,0), (-1,-1), 2),
        ('BOTTOMPADDING', (0,0), (-1,-1), 2),
        ('LEFTPADDING', (0,0), (-1,-1), 4),
        ('RIGHTPADDING', (0,0), (-1,-1), 4),
    ]))
    story.append(t_meta)
    story.append(Spacer(1, 3))

    # 1. Executive Summary & Remediation Deliverables
    story.append(Paragraph("1. Executive Summary & Audit Remediation Deliverables", h1_style))
    story.append(Paragraph(
        "During <b>Week 13 (Days 73–78)</b>, LearnPulse established institutional-grade platform governance, administrative control, real-time platform analytics, and operational AI telemetry. Following an independent compliance audit that evaluated Days 73–78 against all mentor-assigned criteria, a targeted remediation plan was executed to resolve all identified findings:",
        body_style
    ))
    story.append(Paragraph("• <b>P1 — Complete Public Password Reset Lifecycle:</b> Implemented public endpoints <code>POST /api/auth/forgot-password</code> and <code>POST /api/auth/reset-password</code>. Generates 256-bit secure tokens, persists SHA-256 hashes with 24-hr expiry and single-use enforcement, and dispatches simulated email via <code>EmailService</code>. Rejects consumed, expired, or identical passwords.", bullet_style))
    story.append(Paragraph("• <b>P2 — Downstream Teacher Verification Enforcement:</b> Created <code>TeacherVerificationGuard</code> and enforced verification on document uploads (<code>/api/teacher/upload-pdf</code>, <code>/api/teacher/upload-note</code>), quiz authoring/publishing (<code>/api/teacher/quizzes</code>), AI quiz generation (<code>/api/ai/generate-quiz</code>), and student enrollment (<code>/api/teacher/enrollment/subjects/{id}/students</code>). Unaccredited teachers are blocked with HTTP 403 Forbidden.", bullet_style))
    story.append(Paragraph("• <b>P3 — Row-Level Pessimistic Concurrency Guard:</b> Configured <code>findActiveAdminsForUpdate(Role.ADMIN)</code> with <code>@Lock(PESSIMISTIC_WRITE)</code> to serialize concurrent admin demotions/deactivations, strictly guaranteeing <code>activeAdmins &gt; 1</code> and returning HTTP 409 Conflict if attempting to remove the final administrator.", bullet_style))
    story.append(Paragraph("• <b>P4 — AI Token Provenance & Local Inference Economics:</b> Overloaded telemetry tracking to capture <code>usageProvenance</code> (<code>PROVIDER_REPORTED</code> vs <code>LOCAL_ESTIMATION</code>) across all 7 AI features. Local Ollama cost calibrated to $0.00 direct billing with cloud comparative reference rates. Telemetry exceptions caught with <code>catch (Exception e)</code> to avoid swallowing JVM errors.", bullet_style))
    story.append(Paragraph("• <b>P5 — Native DB Registration Trends:</b> Replaced in-memory entity scanning with PostgreSQL native aggregation <code>countRegistrationsByDayNative</code> utilizing <code>date_trunc('day', created_at)</code>.", bullet_style))
    story.append(Paragraph("• <b>P6 — Paginated AI Telemetry UI Table:</b> Connected <code>AdminDashboardPage.tsx</code> to <code>getAiTelemetry(page, size)</code> with interactive pagination, latency tracking, token breakdown, and provenance badges. Corrected terminology to 'Total Registered Users' with enabled account count subtext.", bullet_style))
    story.append(Paragraph("• <b>P7 — Full Test Suite & Build Verification:</b> <b>235 / 235 automated tests PASS (100% BUILD SUCCESS in 2m 37s)</b> across 33 test classes. Frontend Vite production build cleanly transformed 1,697 modules with 0 errors.", bullet_style))

    story.append(Spacer(1, 3))

    # 2. Architecture & Day-by-Day Implementation
    story.append(Paragraph("2. Architecture & Day-by-Day Implementation Breakdown", h1_style))
    
    story.append(Paragraph("2.1 Day 73 — Admin User & Role Management APIs", h2_style))
    story.append(Paragraph(
        "Built <code>AdminUserController.java</code> and <code>AdminUserService.java</code> backed by JPA repositories under <code>@PreAuthorize(\"hasRole('ADMIN')\")</code>. Features include:",
        body_style
    ))
    story.append(Paragraph("• <b>Paginated User Directory (<code>GET /api/admin/users</code>):</b> Dynamic filtering by Role, enabled status, and keyword search across email, first name, and last name.", bullet_style))
    story.append(Paragraph("• <b>User Metrics (<code>GET /api/admin/users/{id}</code>):</b> Returns full profile metadata paired with relational counts for enrollments, assigned classes, completed quizzes, and uploaded materials.", bullet_style))
    story.append(Paragraph("• <b>Role Governance & Concurrency:</b> Blocks self-demotion (HTTP 400). Uses row-level pessimistic locking (<code>PESSIMISTIC_WRITE</code>) on active admins, aborting with HTTP 409 Conflict if active admin count &le; 1.", bullet_style))
    story.append(Paragraph("• <b>Password Reset Workflow:</b> Generates 256-bit token; persists SHA-256 hash in <code>PasswordResetToken</code> with 24-hr expiry; triggers simulated email dispatch via <code>EmailService</code>.", bullet_style))
    story.append(Paragraph("• <b>Account Deactivation & Immediate Revocation:</b> Immediately blocks deactivated users by validating <code>userDetails.isEnabled()</code> inside <code>JwtAuthenticationFilter.java</code>, returning HTTP 401 Unauthorized instantly.", bullet_style))

    story.append(Paragraph("2.2 Day 74 — Platform Analytics Backend", h2_style))
    story.append(Paragraph(
        "Implemented <code>AdminAnalyticsController.java</code> and <code>AdminAnalyticsService.java</code> using high-efficiency SQL aggregation:",
        body_style
    ))
    story.append(Paragraph("• <b>Summary Metrics (<code>GET /api/admin/analytics/summary</code>):</b> Real-time counts of total users, active users, students, teachers, admins, subjects, quizzes, and global quiz average score.", bullet_style))
    story.append(Paragraph("• <b>Registration Velocity (<code>GET /api/admin/analytics/user-activity</code>):</b> 14-day chronological registration breakdown with zero-filling, powered by native PostgreSQL <code>date_trunc</code> aggregation.", bullet_style))
    story.append(Paragraph("• <b>Course Activity (<code>GET /api/admin/analytics/course-activity</code>):</b> Per-subject breakdown of active students, curriculum chapters, documents, and quizzes.", bullet_style))

    story.append(Paragraph("2.3 Day 75 — AI Operational Telemetry & Usage Tracking", h2_style))
    story.append(Paragraph(
        "Created <code>AiTelemetryRecord.java</code> and <code>AiTelemetryService.java</code>. Instrumented all 7 LearnPulse AI operations (Tutor, Quiz Gen, RAG, Flashcards, Explainer, Study Plan, Summarizer):",
        body_style
    ))
    story.append(Paragraph("• <b>Failure Isolation:</b> Telemetry recording uses independent transaction boundaries and catches <code>Exception</code>; persistence errors are logged but never interrupt student/teacher operations.", bullet_style))
    story.append(Paragraph("• <b>Token Modeling & Economics:</b> Direct Ollama cost is $0.00; cloud reference cost calibrated at $0.15/1M input and $0.60/1M output. Provenance tracked as <code>PROVIDER_REPORTED</code> or <code>LOCAL_ESTIMATION</code>.", bullet_style))
    story.append(Paragraph("• <b>Telemetry Endpoints:</b> <code>GET /api/admin/analytics/ai-usage</code> (aggregated KPI summary) and <code>GET /api/admin/analytics/ai-telemetry</code> (paginated operational audit logs).", bullet_style))

    story.append(Paragraph("2.4 Day 76 — Administrative Frontend & Content Governance", h2_style))
    story.append(Paragraph(
        "Engineered React 19 user interfaces adhering to LearnPulse design tokens:",
        body_style
    ))
    story.append(Paragraph("• <code>AdminUsersPage.tsx</code>: Interactive user management table with role badges, search filter, user detail drawer, role change modal, deactivation confirmation modal, and password reset trigger.", bullet_style))
    story.append(Paragraph("• <code>AdminTeachersPage.tsx</code>: Dedicated faculty verification portal displaying pending verification queues, department affiliations, approval triggers, and rejection rationale modals.", bullet_style))
    story.append(Paragraph("• <code>AdminContentModerationPage.tsx</code>: Dual-tab content moderation portal allowing administrators to toggle document active visibility and quiz publication status across all courses.", bullet_style))

    story.append(Paragraph("2.5 Day 77 — Interactive Analytics Dashboard with Chart.js", h2_style))
    story.append(Paragraph(
        "Created <code>AdminDashboardPage.tsx</code> integrating Chart.js and React-Chartjs-2. Registered core chart controllers (ArcElement, BarElement, LineElement, PointElement, CategoryScale, LinearScale, Tooltip, Legend). Features 4 summary KPI cards, 5 charts (User Roles Doughnut, 14-Day Registration Line, AI Feature Bar, AI Health Doughnut, Course Resource Bar), and paginated live AI operational audit table.",
        body_style
    ))

    story.append(Spacer(1, 3))

    # 3. Complete API Specifications Table
    story.append(Paragraph("3. Complete API & Security Specification", h1_style))
    
    api_table_data = [
        [
            Paragraph("<b>HTTP Method & Route</b>", table_header_style),
            Paragraph("<b>RBAC</b>", table_header_style),
            Paragraph("<b>Description & Security Policy</b>", table_header_style),
            Paragraph("<b>Status</b>", table_header_style)
        ],
        [
            Paragraph("<code>POST /api/auth/forgot-password</code>", table_cell_bold),
            Paragraph("Public", table_cell_style),
            Paragraph("Generates 256-bit token; persists SHA-256 hash in DB; dispatches email.", table_cell_style),
            Paragraph("200 OK", table_cell_bold)
        ],
        [
            Paragraph("<code>POST /api/auth/reset-password</code>", table_cell_bold),
            Paragraph("Public", table_cell_style),
            Paragraph("Consumes single-use token; validates 24-hr expiry and password difference.", table_cell_style),
            Paragraph("200 / 400 / 404", table_cell_bold)
        ],
        [
            Paragraph("<code>GET /api/admin/users</code>", table_cell_bold),
            Paragraph("ADMIN", table_cell_style),
            Paragraph("Paginated user list with role, status, and text search.", table_cell_style),
            Paragraph("200 OK", table_cell_bold)
        ],
        [
            Paragraph("<code>GET /api/admin/users/{id}</code>", table_cell_bold),
            Paragraph("ADMIN", table_cell_style),
            Paragraph("User profile combined with LMS activity metrics.", table_cell_style),
            Paragraph("200 OK / 404", table_cell_bold)
        ],
        [
            Paragraph("<code>PATCH /api/admin/users/{id}/role</code>", table_cell_bold),
            Paragraph("ADMIN", table_cell_style),
            Paragraph("Role update with self-demotion guard (400) and pessimistic lock last-admin guard (409).", table_cell_style),
            Paragraph("200 / 400 / 409", table_cell_bold)
        ],
        [
            Paragraph("<code>POST /api/admin/users/{id}/password-reset</code>", table_cell_bold),
            Paragraph("ADMIN", table_cell_style),
            Paragraph("Admin password reset dispatch; stores SHA-256 hash in DB with 24-hr expiry.", table_cell_style),
            Paragraph("200 OK", table_cell_bold)
        ],
        [
            Paragraph("<code>PATCH /api/admin/users/{id}/deactivate</code>", table_cell_bold),
            Paragraph("ADMIN", table_cell_style),
            Paragraph("Deactivates account; immediately invalidates existing JWT tokens (401).", table_cell_style),
            Paragraph("200 / 400 / 409", table_cell_bold)
        ],
        [
            Paragraph("<code>PATCH /api/admin/users/{id}/reactivate</code>", table_cell_bold),
            Paragraph("ADMIN", table_cell_style),
            Paragraph("Restores user access; enables credential validation.", table_cell_style),
            Paragraph("200 OK", table_cell_bold)
        ],
        [
            Paragraph("<code>GET /api/admin/teachers</code>", table_cell_bold),
            Paragraph("ADMIN", table_cell_style),
            Paragraph("Lists faculty members with verification status (PENDING/APPROVED/REJECTED).", table_cell_style),
            Paragraph("200 OK", table_cell_bold)
        ],
        [
            Paragraph("<code>POST /api/admin/teachers/{id}/approve</code>", table_cell_bold),
            Paragraph("ADMIN", table_cell_style),
            Paragraph("Approves faculty member verification.", table_cell_style),
            Paragraph("200 OK", table_cell_bold)
        ],
        [
            Paragraph("<code>POST /api/admin/teachers/{id}/reject</code>", table_cell_bold),
            Paragraph("ADMIN", table_cell_style),
            Paragraph("Rejects verification; records audit rationale in database.", table_cell_style),
            Paragraph("200 / 400", table_cell_bold)
        ],
        [
            Paragraph("<code>PATCH /api/admin/content/documents/{id}/status</code>", table_cell_bold),
            Paragraph("ADMIN", table_cell_style),
            Paragraph("Toggles active syllabus document visibility across LMS.", table_cell_style),
            Paragraph("200 OK", table_cell_bold)
        ],
        [
            Paragraph("<code>PATCH /api/admin/content/quizzes/{id}/status</code>", table_cell_bold),
            Paragraph("ADMIN", table_cell_style),
            Paragraph("Toggles quiz publication status (DRAFT vs PUBLISHED).", table_cell_style),
            Paragraph("200 OK", table_cell_bold)
        ],
        [
            Paragraph("<code>GET /api/admin/analytics/summary</code>", table_cell_bold),
            Paragraph("ADMIN", table_cell_style),
            Paragraph("Platform-wide executive metrics and global quiz average score.", table_cell_style),
            Paragraph("200 OK", table_cell_bold)
        ],
        [
            Paragraph("<code>GET /api/admin/analytics/user-activity</code>", table_cell_bold),
            Paragraph("ADMIN", table_cell_style),
            Paragraph("14-day trailing user registration velocity via native PostgreSQL query.", table_cell_style),
            Paragraph("200 OK", table_cell_bold)
        ],
        [
            Paragraph("<code>GET /api/admin/analytics/course-activity</code>", table_cell_bold),
            Paragraph("ADMIN", table_cell_style),
            Paragraph("Resource distribution per subject (students, chapters, documents, quizzes).", table_cell_style),
            Paragraph("200 OK", table_cell_bold)
        ],
        [
            Paragraph("<code>GET /api/admin/analytics/ai-usage</code>", table_cell_bold),
            Paragraph("ADMIN", table_cell_style),
            Paragraph("Aggregated AI operations, success rate, token volume, and cost summary.", table_cell_style),
            Paragraph("200 OK", table_cell_bold)
        ],
        [
            Paragraph("<code>GET /api/admin/analytics/ai-telemetry</code>", table_cell_bold),
            Paragraph("ADMIN", table_cell_style),
            Paragraph("Paginated operational telemetry logs with latency, model, tokens, and cost.", table_cell_style),
            Paragraph("200 OK", table_cell_bold)
        ]
    ]

    t_api = Table(api_table_data, colWidths=[150, 48, 256, 78])
    t_api.setStyle(TableStyle([
        ('BACKGROUND', (0,0), (-1,0), primary),
        ('GRID', (0,0), (-1,-1), 0.5, border_col),
        ('VALIGN', (0,0), (-1,-1), 'MIDDLE'),
        ('ROWBACKGROUNDS', (0,1), (-1,-1), [colors.white, alt_bg]),
        ('TOPPADDING', (0,0), (-1,-1), 1.8),
        ('BOTTOMPADDING', (0,0), (-1,-1), 1.8),
        ('LEFTPADDING', (0,0), (-1,-1), 3),
        ('RIGHTPADDING', (0,0), (-1,-1), 3),
    ]))
    story.append(t_api)

    story.append(Spacer(1, 3))

    # 4. Automated Verification Matrix
    story.append(Paragraph("4. Automated Verification Matrix & Test Results (Day 78 Post-Remediation)", h1_style))
    story.append(Paragraph(
        "Full regression verification was executed against the active application container and PostgreSQL database (<code>postgres-pgvector:5432</code>).",
        body_style
    ))

    test_matrix_data = [
        [
            Paragraph("<b>Test Suite Class</b>", table_header_style),
            Paragraph("<b>Target Functional Scope</b>", table_header_style),
            Paragraph("<b>Methods</b>", table_header_style),
            Paragraph("<b>Execution Time</b>", table_header_style),
            Paragraph("<b>Result</b>", table_header_style)
        ],
        [
            Paragraph("<code>AdminUserManagementTest</code>", table_cell_bold),
            Paragraph("User list, metrics, profile update, role governance, last-admin guard, password reset, deactivation & immediate JWT revocation", table_cell_style),
            Paragraph("12", table_cell_style),
            Paragraph("8.4s", table_cell_style),
            Paragraph("<font color='#059669'><b>PASS (12/12)</b></font>", table_cell_style)
        ],
        [
            Paragraph("<code>AdminAnalyticsTest</code>", table_cell_bold),
            Paragraph("Platform summary metrics, 14-day registration trends (native query), course activity breakdown, non-admin RBAC 403 enforcement", table_cell_style),
            Paragraph("4", table_cell_style),
            Paragraph("1.2s", table_cell_style),
            Paragraph("<font color='#059669'><b>PASS (4/4)</b></font>", table_cell_style)
        ],
        [
            Paragraph("<code>AiTelemetryTest</code>", table_cell_bold),
            Paragraph("Aggregated AI usage summary, paginated telemetry logs, usage provenance tracking, failure isolation, RBAC 403 enforcement", table_cell_style),
            Paragraph("5", table_cell_style),
            Paragraph("1.3s", table_cell_style),
            Paragraph("<font color='#059669'><b>PASS (5/5)</b></font>", table_cell_style)
        ],
        [
            Paragraph("<code>AdminContentModerationTest</code>", table_cell_bold),
            Paragraph("Faculty verification list, approve, reject with rationale, document active toggle, quiz publication toggle, RBAC 403 enforcement", table_cell_style),
            Paragraph("6", table_cell_style),
            Paragraph("2.8s", table_cell_style),
            Paragraph("<font color='#059669'><b>PASS (6/6)</b></font>", table_cell_style)
        ],
        [
            Paragraph("<code>AuthPasswordResetTest</code>", table_cell_bold),
            Paragraph("Public password reset lifecycle, SHA-256 token hashing, 24-hr expiry, single-use consumed rejection, identical password rejection", table_cell_style),
            Paragraph("6", table_cell_style),
            Paragraph("2.1s", table_cell_style),
            Paragraph("<font color='#059669'><b>PASS (6/6)</b></font>", table_cell_style)
        ],
        [
            Paragraph("<code>TeacherVerificationEnforcementTest</code>", table_cell_bold),
            Paragraph("Downstream TeacherVerificationGuard on document upload, quiz creation/publishing, AI quiz generation, and student enrollment", table_cell_style),
            Paragraph("6", table_cell_style),
            Paragraph("2.4s", table_cell_style),
            Paragraph("<font color='#059669'><b>PASS (6/6)</b></font>", table_cell_style)
        ],
        [
            Paragraph("<code>AdminConcurrencyTest</code>", table_cell_bold),
            Paragraph("Multithreaded pessimistic locking (PESSIMISTIC_WRITE) serializing concurrent admin demotions and throwing HTTP 409 Conflict", table_cell_style),
            Paragraph("1", table_cell_style),
            Paragraph("1.5s", table_cell_style),
            Paragraph("<font color='#059669'><b>PASS (1/1)</b></font>", table_cell_style)
        ],
        [
            Paragraph("<b>Milestone Regression Suites</b>", table_cell_bold),
            Paragraph("Weeks 1–12 & Week 11.5 full regression (Auth, Assessment, Enrollment, AI Quiz, Flashcards, Explainer, RAG, Chat, Storage, etc.)", table_cell_style),
            Paragraph("195", table_cell_style),
            Paragraph("137.3s", table_cell_style),
            Paragraph("<font color='#059669'><b>PASS (195/195)</b></font>", table_cell_style)
        ],
        [
            Paragraph("<b>TOTAL AUTOMATED SUITE</b>", table_cell_bold),
            Paragraph("<b>Complete LearnPulse Automated Test Portfolio (33 Test Classes)</b>", table_cell_bold),
            Paragraph("<b>235</b>", table_cell_bold),
            Paragraph("<b>2m 37s</b>", table_cell_bold),
            Paragraph("<font color='#059669'><b>BUILD SUCCESS</b></font>", table_cell_bold)
        ]
    ]

    t_test = Table(test_matrix_data, colWidths=[140, 230, 42, 58, 62])
    t_test.setStyle(TableStyle([
        ('BACKGROUND', (0,0), (-1,0), primary),
        ('GRID', (0,0), (-1,-1), 0.5, border_col),
        ('VALIGN', (0,0), (-1,-1), 'MIDDLE'),
        ('ROWBACKGROUNDS', (0,1), (-1,-2), [colors.white, alt_bg]),
        ('BACKGROUND', (0,-1), (-1,-1), colors.HexColor("#E2E8F0")),
        ('TOPPADDING', (0,0), (-1,-1), 1.8),
        ('BOTTOMPADDING', (0,0), (-1,-1), 1.8),
        ('LEFTPADDING', (0,0), (-1,-1), 3),
        ('RIGHTPADDING', (0,0), (-1,-1), 3),
    ]))
    story.append(t_test)

    story.append(Spacer(1, 3))

    # 5. Conclusion & Mentor Sign-Off
    story.append(Paragraph("5. Conclusion & Mentor Sign-Off Readiness", h1_style))
    story.append(Paragraph(
        "LearnPulse Week 13: Administrative Dashboard, User Governance & System Analytics satisfies all institutional specifications with 100% test pass rates (235/235), 0 errors, and zero regressions across all prior milestones. All remediations have been verified with dedicated unit, integration, and multithreaded concurrency tests. No Git commits or pushes were made.",
        body_style
    ))

    doc.build(story, canvasmaker=NumberedCanvas)
    print(f"Generated {filename} successfully.")

if __name__ == "__main__":
    build_pdf()
