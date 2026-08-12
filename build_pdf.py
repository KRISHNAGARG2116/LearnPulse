import os
import re
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
        if self._pageNumber == 1:
            return  # Suppress running header & footer on cover page

        self.saveState()
        self.setFont("Helvetica", 8)
        self.setFillColor(colors.HexColor("#64748b"))

        # Running Header
        self.drawString(54, 750, "LearnPulse AI — Week 1 Master Engineering Design Package")
        self.setStrokeColor(colors.HexColor("#cbd5e1"))
        self.setLineWidth(0.5)
        self.line(54, 742, 558, 742)

        # Running Footer
        page_text = f"Page {self._pageNumber} of {page_count}"
        self.drawRightString(558, 36, page_text)
        self.drawString(54, 36, "CONFIDENTIAL & PROPRIETARY — ACADEMIC & TECHNICAL SPECIFICATION")
        self.line(54, 48, 558, 48)

        self.restoreState()


def build_pdf(md_path, pdf_path):
    with open(md_path, "r", encoding="utf-8") as f:
        md_text = f.read()

    doc = SimpleDocTemplate(
        pdf_path,
        pagesize=letter,
        leftMargin=54,
        rightMargin=54,
        topMargin=54,
        bottomMargin=54
    )

    styles = getSampleStyleSheet()

    # Custom Typography Styles
    title_style = ParagraphStyle(
        "CoverTitle",
        parent=styles["Normal"],
        fontName="Helvetica-Bold",
        fontSize=26,
        leading=32,
        textColor=colors.HexColor("#0f172a"),
        spaceAfter=10
    )
    subtitle_style = ParagraphStyle(
        "CoverSubtitle",
        parent=styles["Normal"],
        fontName="Helvetica",
        fontSize=14,
        leading=18,
        textColor=colors.HexColor("#2563eb"),
        spaceAfter=30
    )
    meta_label_style = ParagraphStyle(
        "MetaLabel",
        parent=styles["Normal"],
        fontName="Helvetica-Bold",
        fontSize=10,
        leading=14,
        textColor=colors.HexColor("#1e293b")
    )
    meta_val_style = ParagraphStyle(
        "MetaVal",
        parent=styles["Normal"],
        fontName="Helvetica",
        fontSize=10,
        leading=14,
        textColor=colors.HexColor("#475569")
    )
    h1_style = ParagraphStyle(
        "DocH1",
        parent=styles["Normal"],
        fontName="Helvetica-Bold",
        fontSize=18,
        leading=22,
        textColor=colors.HexColor("#0f172a"),
        spaceBefore=18,
        spaceAfter=8,
        keepWithNext=True
    )
    h2_style = ParagraphStyle(
        "DocH2",
        parent=styles["Normal"],
        fontName="Helvetica-Bold",
        fontSize=13,
        leading=17,
        textColor=colors.HexColor("#1e293b"),
        spaceBefore=14,
        spaceAfter=6,
        keepWithNext=True
    )
    h3_style = ParagraphStyle(
        "DocH3",
        parent=styles["Normal"],
        fontName="Helvetica-Bold",
        fontSize=11,
        leading=15,
        textColor=colors.HexColor("#2563eb"),
        spaceBefore=10,
        spaceAfter=4,
        keepWithNext=True
    )
    body_style = ParagraphStyle(
        "DocBody",
        parent=styles["Normal"],
        fontName="Helvetica",
        fontSize=9.5,
        leading=14,
        textColor=colors.HexColor("#334155"),
        spaceAfter=6
    )
    bullet_style = ParagraphStyle(
        "DocBullet",
        parent=body_style,
        leftIndent=15,
        firstLineIndent=-10,
        spaceAfter=4
    )
    callout_style = ParagraphStyle(
        "DocCallout",
        parent=styles["Normal"],
        fontName="Helvetica-Oblique",
        fontSize=9,
        leading=13,
        textColor=colors.HexColor("#1e293b")
    )
    code_style = ParagraphStyle(
        "DocCode",
        parent=styles["Normal"],
        fontName="Courier",
        fontSize=7.5,
        leading=10,
        textColor=colors.HexColor("#0f172a")
    )
    table_cell_style = ParagraphStyle(
        "TableCell",
        parent=styles["Normal"],
        fontName="Helvetica",
        fontSize=8.5,
        leading=11,
        textColor=colors.HexColor("#334155")
    )
    table_header_style = ParagraphStyle(
        "TableHeader",
        parent=styles["Normal"],
        fontName="Helvetica-Bold",
        fontSize=8.5,
        leading=11,
        textColor=colors.white
    )

    story = []

    # 1. Cover Page Assembly
    story.append(Spacer(1, 40))
    story.append(Paragraph("LearnPulse AI", title_style))
    story.append(Paragraph("Enterprise AI-Powered Learning Management System", subtitle_style))
    story.append(HRFlowable(width="100%", thickness=3, color=colors.HexColor("#2563eb"), spaceAfter=40))
    
    story.append(Paragraph("DOCUMENT DESIGN PACKAGE", ParagraphStyle("CovType", fontName="Helvetica-Bold", fontSize=14, textColor=colors.HexColor("#64748b"), spaceAfter=15)))
    story.append(Paragraph("Master Week 1 Requirements, Architecture, Database, Frontend & AI Subsystem Specifications", ParagraphStyle("CovDesc", fontName="Helvetica", fontSize=11, leading=16, textColor=colors.HexColor("#334155"), spaceAfter=40)))

    meta_table_data = [
        [Paragraph("Document Title:", meta_label_style), Paragraph("Week 1 Master Engineering Design Package", meta_val_style)],
        [Paragraph("Document Version:", meta_label_style), Paragraph("1.0.0-FINAL (Approved for Kickoff)", meta_val_style)],
        [Paragraph("Release Date:", meta_label_style), Paragraph("August 5, 2026", meta_val_style)],
        [Paragraph("Prepared By:", meta_label_style), Paragraph("Senior Solution Architecture & Engineering Team", meta_val_style)],
        [Paragraph("Target Audience:", meta_label_style), Paragraph("Academic Evaluators, Technical Mentors, & Clients", meta_val_style)],
        [Paragraph("Core Stack:", meta_label_style), Paragraph("React SPA, Spring Boot 3.x, PostgreSQL 16 + pgvector, S3, RAG", meta_val_style)],
    ]
    t_meta = Table(meta_table_data, colWidths=[130, 374])
    t_meta.setStyle(TableStyle([
        ('VALIGN', (0,0), (-1,-1), 'MIDDLE'),
        ('BOTTOMPADDING', (0,0), (-1,-1), 6),
        ('TOPPADDING', (0,0), (-1,-1), 6),
        ('LINEBELOW', (0,0), (-1,-1), 0.5, colors.HexColor("#e2e8f0")),
    ]))
    story.append(t_meta)
    story.append(PageBreak())

    # Parse Markdown Blocks
    lines = md_text.split("\n")
    i = 0
    in_code_block = False
    code_lines = []
    in_table = False
    table_rows = []

    def flush_table(rows):
        if not rows:
            return None
        formatted_data = []
        for r_idx, row in enumerate(rows):
            formatted_row = []
            for cell in row:
                st = table_header_style if r_idx == 0 else table_cell_style
                # Clean markdown styling inside table cells
                clean_cell = cell.replace("**", "").replace("`", "")
                formatted_row.append(Paragraph(clean_cell, st))
            formatted_data.append(formatted_row)
        
        num_cols = max(len(r) for r in formatted_data)
        col_w = 504.0 / num_cols
        col_widths = [col_w] * num_cols

        t = Table(formatted_data, colWidths=col_widths)
        ts = [
            ('BACKGROUND', (0,0), (-1,0), colors.HexColor("#1e293b")),
            ('ALIGN', (0,0), (-1,-1), 'LEFT'),
            ('VALIGN', (0,0), (-1,-1), 'TOP'),
            ('GRID', (0,0), (-1,-1), 0.5, colors.HexColor("#cbd5e1")),
            ('TOPPADDING', (0,0), (-1,-1), 5),
            ('BOTTOMPADDING', (0,0), (-1,-1), 5),
            ('LEFTPADDING', (0,0), (-1,-1), 6),
            ('RIGHTPADDING', (0,0), (-1,-1), 6),
        ]
        for r in range(1, len(formatted_data)):
            if r % 2 == 1:
                ts.append(('BACKGROUND', (0, r), (-1, r), colors.HexColor("#f8fafc")))
            else:
                ts.append(('BACKGROUND', (0, r), (-1, r), colors.HexColor("#ffffff")))
        t.setStyle(TableStyle(ts))
        return t

    while i < len(lines):
        line = lines[i]

        # Handle Page Breaks
        if '<div style="page-break-before: always;"></div>' in line or "---" == line.strip():
            if in_table and table_rows:
                t_obj = flush_table(table_rows)
                if t_obj: story.append(t_obj)
                table_rows = []
                in_table = False
            if 'page-break' in line:
                story.append(PageBreak())
            else:
                story.append(Spacer(1, 8))
                story.append(HRFlowable(width="100%", thickness=0.5, color=colors.HexColor("#cbd5e1"), spaceAfter=12))
            i += 1
            continue

        # Handle Code Blocks & Diagrams
        if line.strip().startswith("```"):
            if in_code_block:
                in_code_block = False
                code_content = "\n".join(code_lines)
                code_lines = []
                p_code = Paragraph(code_content.replace("\n", "<br/>").replace(" ", "&nbsp;"), code_style)
                # Wrap in preformatted box table
                t_box = Table([[p_code]], colWidths=[504])
                t_box.setStyle(TableStyle([
                    ('BACKGROUND', (0,0), (-1,-1), colors.HexColor("#f1f5f9")),
                    ('BOX', (0,0), (-1,-1), 0.5, colors.HexColor("#cbd5e1")),
                    ('TOPPADDING', (0,0), (-1,-1), 8),
                    ('BOTTOMPADDING', (0,0), (-1,-1), 8),
                    ('LEFTPADDING', (0,0), (-1,-1), 10),
                    ('RIGHTPADDING', (0,0), (-1,-1), 10),
                ]))
                story.append(Spacer(1, 4))
                story.append(t_box)
                story.append(Spacer(1, 8))
            else:
                if in_table and table_rows:
                    t_obj = flush_table(table_rows)
                    if t_obj: story.append(t_obj)
                    table_rows = []
                    in_table = False
                in_code_block = True
                code_lines = []
            i += 1
            continue

        if in_code_block:
            code_lines.append(line)
            i += 1
            continue

        # Handle Tables
        if "|" in line and line.strip().startswith("|") and line.strip().endswith("|"):
            if "---" in line:  # Table header separator line
                i += 1
                continue
            in_table = True
            cells = [c.strip() for c in line.strip().split("|")[1:-1]]
            table_rows.append(cells)
            i += 1
            continue
        else:
            if in_table:
                t_obj = flush_table(table_rows)
                if t_obj: story.append(t_obj)
                table_rows = []
                in_table = False

        # Handle Callout Alerts (> [!NOTE], > [!IMPORTANT], etc.)
        if line.strip().startswith(">"):
            callout_text = line.strip().lstrip(">").strip()
            border_color = colors.HexColor("#2563eb")
            bg_color = colors.HexColor("#eff6ff")
            if "[!IMPORTANT]" in callout_text or "[!WARNING]" in callout_text:
                border_color = colors.HexColor("#d97706")
                bg_color = colors.HexColor("#fffbeb")
            elif "[!SECURITY]" in callout_text:
                border_color = colors.HexColor("#dc2626")
                bg_color = colors.HexColor("#fef2f2")
            elif "[!TIP]" in callout_text:
                border_color = colors.HexColor("#059669")
                bg_color = colors.HexColor("#ecfdf5")

            callout_clean = callout_text.replace("[!NOTE]", "<b>NOTE:</b> ").replace("[!IMPORTANT]", "<b>IMPORTANT:</b> ").replace("[!SECURITY]", "<b>SECURITY POLICY:</b> ").replace("[!TIP]", "<b>BEST PRACTICE:</b> ")
            p_call = Paragraph(callout_clean, callout_style)
            t_call = Table([[p_call]], colWidths=[504])
            t_call.setStyle(TableStyle([
                ('BACKGROUND', (0,0), (-1,-1), bg_color),
                ('LEFTPADDING', (0,0), (-1,-1), 10),
                ('RIGHTPADDING', (0,0), (-1,-1), 10),
                ('TOPPADDING', (0,0), (-1,-1), 8),
                ('BOTTOMPADDING', (0,0), (-1,-1), 8),
                ('LINELEFT', (0,0), (-1,-1), 3, border_color),
            ]))
            story.append(Spacer(1, 4))
            story.append(t_call)
            story.append(Spacer(1, 8))
            i += 1
            continue

        # Handle Headings
        if line.startswith("# "):
            story.append(Paragraph(line[2:].strip(), h1_style))
        elif line.startswith("## "):
            story.append(Paragraph(line[3:].strip(), h2_style))
        elif line.startswith("### "):
            story.append(Paragraph(line[4:].strip(), h3_style))
        elif line.startswith("#### "):
            story.append(Paragraph(line[5:].strip(), ParagraphStyle("DocH4", parent=h3_style, fontSize=10, textColor=colors.HexColor("#0f172a"))))
        # Handle Bullet Points
        elif line.strip().startswith("* ") or line.strip().startswith("- "):
            bullet_text = line.strip()[2:].strip()
            # Clean markdown bold/code tags
            formatted_bullet = re.sub(r'\*\*(.*?)\*\*', r'<b>\1</b>', bullet_text)
            formatted_bullet = re.sub(r'`(.*?)`', r'<font face="Courier">\1</font>', formatted_bullet)
            story.append(Paragraph(f"• {formatted_bullet}", bullet_style))
        # Handle Paragraphs
        elif line.strip():
            text = line.strip()
            formatted_text = re.sub(r'\*\*(.*?)\*\*', r'<b>\1</b>', text)
            formatted_text = re.sub(r'`(.*?)`', r'<font face="Courier">\1</font>', formatted_text)
            story.append(Paragraph(formatted_text, body_style))

        i += 1

    if in_table and table_rows:
        t_obj = flush_table(table_rows)
        if t_obj: story.append(t_obj)

    doc.build(story, canvasmaker=NumberedCanvas)
    print(f"PDF Successfully Generated: {pdf_path}")

if __name__ == "__main__":
    md_file = "/Users/krishnagarg/lms internship/Engineering_Design_Package.md"
    pdf_file = "/Users/krishnagarg/lms internship/Engineering_Design_Package.pdf"
    build_pdf(md_file, pdf_file)
