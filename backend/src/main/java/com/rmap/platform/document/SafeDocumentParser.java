package com.rmap.platform.document;

import com.rmap.platform.common.ApiException;
import com.rmap.platform.config.RmapProperties;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Component
public class SafeDocumentParser {
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "pdf", "docx", "txt", "md", "csv", "xlsx", "png", "jpg", "jpeg", "webp", "zip"
    );
    private static final Pattern SECRET_PATTERN = Pattern.compile(
            "(?i)(api[_-]?key|secret|password|bearer|private[_-]?key)\\s*[:=]\\s*\\S+"
    );

    private final RmapProperties properties;

    public SafeDocumentParser(RmapProperties properties) {
        this.properties = properties;
    }

    public record ParsedDocumentResult(
            String sanitizedFileName,
            String detectedType,
            List<ProjectDocument.DocumentChunk> chunks,
            List<String> securityWarnings,
            String summary
    ) {}

    public ParsedDocumentResult parse(String originalFileName, byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "EMPTY_FILE", "Uploaded file is empty.");
        }
        long maxBytes = properties.uploads() != null ? properties.uploads().maxFileBytes() : 15_728_640L;
        if (bytes.length > maxBytes) {
            throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "FILE_TOO_LARGE", "File exceeds maximum allowed size.");
        }

        String sanitized = sanitizeFileName(originalFileName);
        String ext = extensionOf(sanitized);
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "UNSUPPORTED_FILE_TYPE", "Unsupported file extension: ." + ext);
        }

        List<String> warnings = new ArrayList<>();
        try {
            List<ProjectDocument.DocumentChunk> chunks = switch (ext) {
                case "pdf" -> parsePdf(bytes);
                case "docx" -> parseDocx(bytes);
                case "xlsx" -> parseXlsx(bytes);
                case "zip" -> parseZip(bytes, warnings);
                case "png", "jpg", "jpeg", "webp" -> List.of(new ProjectDocument.DocumentChunk(
                        1, "Image Reference", null, "[Image file: " + sanitized + ", " + bytes.length + " bytes — ready for multimodal analysis]"
                ));
                default -> parsePlainText(bytes, sanitized);
            };

            List<ProjectDocument.DocumentChunk> redacted = new ArrayList<>();
            boolean redactedAny = false;
            for (var c : chunks) {
                String clean = SECRET_PATTERN.matcher(c.text()).replaceAll("$1=[REDACTED]");
                if (!clean.equals(c.text())) {
                    redactedAny = true;
                }
                redacted.add(new ProjectDocument.DocumentChunk(c.pageNumber(), c.sectionTitle(), c.sheetName(), clean));
            }
            if (redactedAny) {
                warnings.add("Potential secret-like tokens were automatically redacted from extracted text.");
            }

            String summary = buildSummary(sanitized, ext.toUpperCase(Locale.ROOT), redacted);
            return new ParsedDocumentResult(sanitized, ext.toUpperCase(Locale.ROOT), redacted, warnings, summary);
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "DOCUMENT_PARSE_FAILED", "Unable to safely parse document: " + ex.getMessage());
        }
    }

    private List<ProjectDocument.DocumentChunk> parsePdf(byte[] bytes) throws IOException {
        List<ProjectDocument.DocumentChunk> chunks = new ArrayList<>();
        try (PDDocument doc = Loader.loadPDF(bytes)) {
            int pages = Math.min(doc.getNumberOfPages(), 150);
            PDFTextStripper stripper = new PDFTextStripper();
            for (int page = 1; page <= pages; page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String text = stripper.getText(doc).trim();
                if (!text.isBlank()) {
                    chunks.add(new ProjectDocument.DocumentChunk(page, "Page " + page, null, truncate(text, 8000)));
                }
            }
        }
        return chunks;
    }

    private List<ProjectDocument.DocumentChunk> parseDocx(byte[] bytes) throws IOException {
        List<ProjectDocument.DocumentChunk> chunks = new ArrayList<>();
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(bytes))) {
            StringBuilder current = new StringBuilder();
            int sectionIndex = 1;
            for (XWPFParagraph para : doc.getParagraphs()) {
                String line = para.getText();
                if (line != null && !line.isBlank()) {
                    current.append(line.trim()).append('\n');
                    if (current.length() >= 3500) {
                        chunks.add(new ProjectDocument.DocumentChunk(sectionIndex, "Section " + sectionIndex, null, current.toString().trim()));
                        current.setLength(0);
                        sectionIndex++;
                    }
                }
            }
            if (!current.isEmpty()) {
                chunks.add(new ProjectDocument.DocumentChunk(sectionIndex, "Section " + sectionIndex, null, current.toString().trim()));
            }
        }
        return chunks;
    }

    private List<ProjectDocument.DocumentChunk> parseXlsx(byte[] bytes) throws IOException {
        List<ProjectDocument.DocumentChunk> chunks = new ArrayList<>();
        DataFormatter formatter = new DataFormatter();
        try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            for (int s = 0; s < Math.min(wb.getNumberOfSheets(), 20); s++) {
                Sheet sheet = wb.getSheetAt(s);
                StringBuilder sb = new StringBuilder();
                int rowLimit = Math.min(sheet.getLastRowNum() + 1, 200);
                for (int r = 0; r < rowLimit; r++) {
                    Row row = sheet.getRow(r);
                    if (row == null) continue;
                    List<String> cells = new ArrayList<>();
                    for (Cell cell : row) {
                        cells.add(formatter.formatCellValue(cell));
                    }
                    sb.append(String.join(" | ", cells)).append('\n');
                }
                chunks.add(new ProjectDocument.DocumentChunk(s + 1, "Sheet: " + sheet.getSheetName(), sheet.getSheetName(), truncate(sb.toString().trim(), 8000)));
            }
        }
        return chunks;
    }

    private List<ProjectDocument.DocumentChunk> parseZip(byte[] bytes, List<String> warnings) throws IOException {
        int maxEntries = properties.uploads() != null ? properties.uploads().maxZipEntries() : 500;
        long maxUncompressed = properties.uploads() != null ? properties.uploads().maxZipUncompressedBytes() : 52_428_800L;

        List<ProjectDocument.DocumentChunk> chunks = new ArrayList<>();
        StringBuilder manifest = new StringBuilder();
        int entryCount = 0;
        long totalBytes = 0;
        int excludedSecrets = 0;

        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                entryCount++;
                if (entryCount > maxEntries) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "ZIP_ARCHIVE_BOMB_BLOCKED", "Archive exceeds maximum allowed entry count.");
                }
                String name = entry.getName();
                if (name.contains("..") || name.startsWith("/") || name.startsWith("\\") || name.contains(":")) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "ZIP_PATH_TRAVERSAL_BLOCKED", "Malicious path traversal detected inside ZIP archive: " + name);
                }
                if (entry.isDirectory()) continue;

                if (isSensitiveEntry(name)) {
                    excludedSecrets++;
                    continue;
                }

                byte[] buf = new byte[4096];
                int read;
                long entryBytes = 0;
                while ((read = zis.read(buf)) != -1) {
                    entryBytes += read;
                    totalBytes += read;
                    if (totalBytes > maxUncompressed) {
                        throw new ApiException(HttpStatus.BAD_REQUEST, "ZIP_ARCHIVE_BOMB_BLOCKED", "Archive exceeds maximum uncompressed size limit.");
                    }
                }
                manifest.append(name).append(" (").append(entryBytes).append(" bytes)\n");
            }
        }
        if (excludedSecrets > 0) {
            warnings.add("Excluded " + excludedSecrets + " sensitive file(s) from ZIP archive.");
        }
        chunks.add(new ProjectDocument.DocumentChunk(1, "ZIP Manifest", null, truncate(manifest.toString().trim(), 8000)));
        return chunks;
    }

    private List<ProjectDocument.DocumentChunk> parsePlainText(byte[] bytes, String fileName) {
        String content = new String(bytes, StandardCharsets.UTF_8).trim();
        List<ProjectDocument.DocumentChunk> chunks = new ArrayList<>();
        int chunkSize = 4000;
        int page = 1;
        for (int i = 0; i < content.length(); i += chunkSize) {
            String part = content.substring(i, Math.min(content.length(), i + chunkSize));
            chunks.add(new ProjectDocument.DocumentChunk(page, fileName + " (Part " + page + ")", null, part));
            page++;
            if (page > 50) break;
        }
        return chunks;
    }

    private boolean isSensitiveEntry(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".env.example") || lower.endsWith(".env.template")) return false;
        return lower.endsWith("/.env") || lower.equals(".env") || lower.contains("/.env.")
                || lower.endsWith(".pem") || lower.endsWith(".key") || lower.endsWith(".p12")
                || lower.contains("secret") || lower.contains("credential");
    }

    private String sanitizeFileName(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_FILE_NAME", "File name is required.");
        }
        if (raw.contains("..") || raw.contains("/") || raw.contains("\\")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "PATH_TRAVERSAL_BLOCKED", "Invalid characters in file name.");
        }
        return raw.trim();
    }

    private String extensionOf(String name) {
        int idx = name.lastIndexOf('.');
        if (idx < 0 || idx == name.length() - 1) return "";
        return name.substring(idx + 1).toLowerCase(Locale.ROOT);
    }

    private String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max);
    }

    private String buildSummary(String name, String type, List<ProjectDocument.DocumentChunk> chunks) {
        int chars = chunks.stream().mapToInt(c -> c.text().length()).sum();
        return "%s (%s) — %d extracted section(s), %d characters.".formatted(name, type, chunks.size(), chars);
    }
}
