package com.learnpulse.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

@Service
@Slf4j
public class DocumentTextExtractionService {

    private final Tika tika;

    public DocumentTextExtractionService() {
        this.tika = new Tika();
    }

    public String extractText(Path filePath, String contentType, String originalFileName) {
        if (filePath == null || !filePath.toFile().exists()) {
            log.error("Text extraction failed: File does not exist at path {}", filePath);
            return "";
        }

        String fileNameLower = originalFileName != null ? originalFileName.toLowerCase() : "";
        File file = filePath.toFile();

        // 1. PDF-Specific Text Extraction via Apache PDFBox
        if (fileNameLower.endsWith(".pdf") || "application/pdf".equalsIgnoreCase(contentType)) {
            try {
                String pdfBoxText = extractTextWithPdfBox(file);
                if (StringUtils.hasText(pdfBoxText)) {
                    log.info("Successfully extracted {} characters from PDF '{}' using Apache PDFBox", pdfBoxText.length(), originalFileName);
                    return pdfBoxText;
                }
            } catch (Exception ex) {
                log.warn("Apache PDFBox extraction warning for '{}': {}. Falling back to Apache Tika.", originalFileName, ex.getMessage());
            }
        }

        // 2. Multi-Format Text Extraction (PDF, DOC, DOCX) via Apache Tika
        try {
            String tikaText = tika.parseToString(file);
            if (StringUtils.hasText(tikaText)) {
                String trimmedText = tikaText.trim();
                log.info("Successfully extracted {} characters from document '{}' using Apache Tika", trimmedText.length(), originalFileName);
                return trimmedText;
            }
        } catch (Exception ex) {
            log.error("Apache Tika extraction failed for file '{}': {}", originalFileName, ex.getMessage());
        }

        log.warn("Text extraction completed with empty result for document '{}'", originalFileName);
        return "";
    }

    private String extractTextWithPdfBox(File file) throws IOException {
        try (PDDocument document = PDDocument.load(file)) {
            if (document.isEncrypted()) {
                log.warn("PDF document is encrypted; skipping PDFBox text extraction");
                return "";
            }
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document).trim();
        }
    }
}
