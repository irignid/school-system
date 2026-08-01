package com.school.service;

import com.school.model.Mark;
import com.school.model.Student;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Generates a single-page A4 report card PDF using Apache PDFBox.
 *
 * Layout (top → bottom):
 *   School header
 *   ── separator ──
 *   Student info block
 *   ── separator ──
 *   Marks table  (Subject | Exam | Mark | Max | % | Grade)
 *   ── separator ──
 *   Attendance summary
 *   ── separator ──
 *   Footer
 */
public class ReportCardService {

    // ── Fonts ─────────────────────────────────────────────────
    private PDType1Font BOLD;
    private PDType1Font REGULAR;
    private PDType1Font ITALIC;

    // ── Page geometry (A4 points) ─────────────────────────────
    private static final float PAGE_W    = PDRectangle.A4.getWidth();   // 595
    private static final float PAGE_H    = PDRectangle.A4.getHeight();  // 842
    private static final float MARGIN    = 50f;
    private static final float CONTENT_W = PAGE_W - MARGIN * 2;

    // ── Colours ───────────────────────────────────────────────
    private static final Color HEADER_BG  = new Color(107, 26,  26);   // maroon
    private static final Color ACCENT     = new Color(200, 168, 75);   // gold
    private static final Color ROW_ALT    = new Color(241, 245, 249);  // slate-100
    private static final Color BORDER     = new Color(203, 213, 225);  // slate-300
    private static final Color TEXT_DARK  = new Color(15,  23,  42);   // slate-900
    private static final Color TEXT_MID   = new Color(71,  85,  105);  // slate-600
    private static final Color GRADE_GOOD = new Color(22,  163, 74);   // green-600
    private static final Color GRADE_WARN = new Color(217, 119, 6);    // amber-600
    private static final Color GRADE_FAIL = new Color(220, 38,  38);   // red-600

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMMM yyyy");

    // ══════════════════════════════════════════════════════════
    //  PUBLIC API
    // ══════════════════════════════════════════════════════════

    /**
     * Generates the report card and saves it to {@code outputFile}.
     *
     * @param student      student model (address field used for class|year display)
     * @param termLabel    e.g. "Term 1  —  2025/2026"
     * @param marks        list of marks with grade symbols
     * @param attendance   int[5]: total, present, absent, late, medical
     * @param schoolName   name printed in the header
     * @param outputFile   destination file (parent dirs must exist)
     */
    public void generate(Student student, String termLabel, List<Mark> marks,
                          int[] attendance, String schoolName, File outputFile) throws IOException {

        try (PDDocument doc = new PDDocument()) {

            // Initialise fonts inside try block (tied to document lifetime)
            BOLD    = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            REGULAR = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            ITALIC  = new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);

            // Load logo from classpath
            PDImageXObject logo = null;
            try (var stream = getClass().getResourceAsStream("/com/school/images/school_logo.png")) {
                if (stream != null) {
                    logo = PDImageXObject.createFromByteArray(doc,
                            stream.readAllBytes(), "school_logo");
                }
            } catch (Exception ignored) {}

            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float y = PAGE_H - MARGIN;

                y = drawHeader      (cs, schoolName, termLabel, y, logo);
                y = drawStudentInfo (cs, student, y);
                y = drawMarksTable  (cs, marks, y);
                y = drawAttendance  (cs, attendance, y);
                    drawFooter      (cs);
            }

            doc.save(outputFile);
        }
    }

    // ══════════════════════════════════════════════════════════
    //  SECTIONS
    // ══════════════════════════════════════════════════════════

    /** Dark header band with school name and "Progress Report Card". */
    private float drawHeader(PDPageContentStream cs, String schoolName,
                             String termLabel, float y,
                             PDImageXObject logo) throws IOException {
        float h = 90f;    // taller band
        fillRect(cs, 0, y - h, PAGE_W, h, HEADER_BG);

        // Logo — 68x68 pts, vertically centred, slight left inset
        float logoSize = 78f;
        float logoX    = MARGIN;
        float logoY    = y - h + (h - logoSize) / 2f;
        if (logo != null) {
            cs.drawImage(logo, logoX, logoY, logoSize, logoSize);
        }

        // Text block starts after logo
        float textX = MARGIN + logoSize + 20;

        // School name — larger
        cs.setNonStrokingColor(new Color(200, 168, 75));
        drawText(cs, BOLD, 22, schoolName, textX, y - 28);

        // Subtitle
        cs.setNonStrokingColor(new Color(200, 168, 75));
        drawText(cs, REGULAR, 10, "Student Progress Report Card", textX, y - 46);

        // Gold separator line under subtitle
        cs.setStrokingColor(new Color(200, 168, 75));
        cs.setLineWidth(0.6f);
        cs.moveTo(textX, y - 52);
        cs.lineTo(PAGE_W - MARGIN, y - 52);
        cs.stroke();

        // Term label — right aligned, top
        cs.setNonStrokingColor(new Color(200, 168, 75));
        float termW = textWidth(BOLD, 11, termLabel);
        drawText(cs, BOLD, 11, termLabel, textX, y - 66);

        // Generated date — right aligned, below term
        String today = "Generated: " + DATE_FMT.format(LocalDate.now());
        cs.setNonStrokingColor(new Color(180, 148, 55));
        float todayW = textWidth(REGULAR, 9, today);
        drawText(cs, REGULAR, 9, today, PAGE_W - MARGIN - todayW, y - 64);

        return y - h - 18;   // more breathing room below header
    }

    /** Student name, class, DOB, gender block. */
    private float drawStudentInfo(PDPageContentStream cs, Student student, float y) throws IOException {
        // Parse class display and year from address field (set by DAO)
        String classDisplay = "—";
        String yearLabel    = "—";
        if (student.getAddress() != null && student.getAddress().contains("|")) {
            String[] parts = student.getAddress().split("\\|", 2);
            classDisplay = parts[0] != null ? parts[0] : "—";
            yearLabel    = parts[1] != null ? parts[1] : "—";
        }

        sectionLabel(cs, "STUDENT INFORMATION", y);
        y -= 20;

        float col1 = MARGIN;
        float col2 = MARGIN + CONTENT_W / 2;

        infoRow(cs, "Full Name",        student.getFullName(),         col1, y);
        infoRow(cs, "Class",            classDisplay,                  col2, y);
        y -= 18;
        String dob = student.getDateOfBirth() != null
                ? DATE_FMT.format(student.getDateOfBirth()) : "—";
        infoRow(cs, "Date of Birth",    dob,                           col1, y);
        infoRow(cs, "Academic Year",    yearLabel,                     col2, y);
        y -= 18;
        infoRow(cs, "Gender",           student.getGenderDisplay(),    col1, y);
        infoRow(cs, "NIC",              student.getNic() != null ? student.getNic() : "—", col2, y);
        y -= 14;

        drawHRule(cs, y);
        return y - 14;
    }

    /** Marks table with alternating row colours and colour-coded grades. */
    private float drawMarksTable(PDPageContentStream cs, List<Mark> marks, float y) throws IOException {
        sectionLabel(cs, "ACADEMIC PERFORMANCE", y);
        y -= 20;

        // Column widths
        float[] colW = { 160, 120, 55, 50, 55, 55 };
        String[] headers = { "Subject", "Exam", "Mark", "Max", "%", "Grade" };

        // Header row
        float x = MARGIN;
        fillRect(cs, MARGIN, y - 16, CONTENT_W, 20, ACCENT);
        cs.setNonStrokingColor(Color.WHITE);
        for (int i = 0; i < headers.length; i++) {
            drawText(cs, BOLD, 9, headers[i], x + 4, y - 10);
            x += colW[i];
        }
        y -= 16;

        if (marks.isEmpty()) {
            cs.setNonStrokingColor(TEXT_MID);
            drawText(cs, ITALIC, 10, "No marks recorded for this term.", MARGIN + 4, y - 16);
            y -= 32;
        } else {
            boolean alt = false;
            for (Mark mk : marks) {
                float rowH = 18f;
                if (alt) fillRect(cs, MARGIN, y - rowH + 2, CONTENT_W, rowH, ROW_ALT);

                x = MARGIN;
                cs.setNonStrokingColor(TEXT_DARK);
                drawText(cs, REGULAR, 9, truncate(mk.getSubjectName(), 28), x + 4, y - 10);
                x += colW[0];
                drawText(cs, REGULAR, 9, truncate(mk.getExamName(), 20),   x + 4, y - 10);
                x += colW[1];
                drawText(cs, REGULAR, 9, formatNum(mk.getMarkObtained()),  x + 4, y - 10);
                x += colW[2];
                drawText(cs, REGULAR, 9, formatNum(mk.getMaxMark()),       x + 4, y - 10);
                x += colW[3];
                drawText(cs, REGULAR, 9, String.format("%.1f", mk.getPercentage()), x + 4, y - 10);
                x += colW[4];

                // Colour-coded grade
                cs.setNonStrokingColor(gradeColor(mk.getGradeSymbol()));
                drawText(cs, BOLD, 10, mk.getGradeSymbol(), x + 4, y - 10);

                y -= rowH;
                alt = !alt;
            }
        }

        // Bottom border
        drawHRule(cs, y - 4);
        return y - 18;
    }

    /** Attendance summary row. */
    private float drawAttendance(PDPageContentStream cs, int[] att, float y) throws IOException {
        // att = { total, present, absent, late, medical }
        sectionLabel(cs, "ATTENDANCE SUMMARY", y);
        y -= 20;

        String[] labels = { "Total Periods", "Present", "Absent", "Late", "Medical Leave", "Attendance %" };
        String[] values = {
                String.valueOf(att[0]),
                String.valueOf(att[1]),
                String.valueOf(att[2]),
                String.valueOf(att[3]),
                String.valueOf(att[4]),
                att[0] > 0 ? String.format("%.1f%%", (double) att[1] / att[0] * 100) : "—"
        };

        float cellW = CONTENT_W / 6f;
        float cellH = 38f;

        // Draw 6 cells
        Color[] cellBg = {
                new Color(226, 232, 240),  // total — slate
                new Color(220, 252, 231),  // present — green
                new Color(254, 226, 226),  // absent — red
                new Color(255, 237, 213),  // late — orange
                new Color(219, 234, 254),  // medical — blue
                new Color(252, 243, 207),  // pct — gold tint bg
        };
        Color[] cellFg = {
                new Color(51,  65,  85),
                new Color(22,  163, 74),
                new Color(220, 38,  38),
                new Color(234, 88,  12),
                new Color(37,  99,  235),
                new Color(200, 168, 75),   // gold fg
        };

        for (int i = 0; i < 6; i++) {
            float cx = MARGIN + i * cellW;
            fillRect(cs, cx, y - cellH, cellW - 2, cellH, cellBg[i]);

            // Value (big)
            cs.setNonStrokingColor(cellFg[i]);
            float vw = textWidth(BOLD, 13, values[i]);
            drawText(cs, BOLD, 13, values[i], cx + (cellW - 2) / 2f - vw / 2f, y - 16);

            // Label (small, centred)
            cs.setNonStrokingColor(TEXT_MID);
            float lw = textWidth(REGULAR, 7, labels[i]);
            drawText(cs, REGULAR, 7, labels[i], cx + (cellW - 2) / 2f - lw / 2f, y - 30);
        }

        drawHRule(cs, y - cellH - 6);
        return y - cellH - 20;
    }

    /** Footer line. */
    private void drawFooter(PDPageContentStream cs) throws IOException {
        float y = MARGIN - 10;
        drawHRule(cs, y + 14);
        cs.setNonStrokingColor(TEXT_MID);
        drawText(cs, ITALIC, 8, "This is a computer-generated document. No signature required.", MARGIN, y);
        String page = "Page 1 of 1";
        float pw = textWidth(REGULAR, 8, page);
        cs.setNonStrokingColor(TEXT_MID);
        drawText(cs, REGULAR, 8, page, PAGE_W - MARGIN - pw, y);
    }

    // ══════════════════════════════════════════════════════════
    //  DRAWING PRIMITIVES
    // ══════════════════════════════════════════════════════════

    private void sectionLabel(PDPageContentStream cs, String text, float y) throws IOException {
        cs.setNonStrokingColor(ACCENT);
        drawText(cs, BOLD, 9, text, MARGIN, y);
        // Underline
        cs.setStrokingColor(ACCENT);
        cs.setLineWidth(0.5f);
        cs.moveTo(MARGIN, y - 3);
        cs.lineTo(MARGIN + CONTENT_W, y - 3);
        cs.stroke();
    }

    private void infoRow(PDPageContentStream cs, String label, String value,
                          float x, float y) throws IOException {
        cs.setNonStrokingColor(TEXT_MID);
        drawText(cs, REGULAR, 9, label + ":", x, y);
        cs.setNonStrokingColor(TEXT_DARK);
        drawText(cs, BOLD, 9, value != null ? value : "—", x + 90, y);
    }

    private void drawHRule(PDPageContentStream cs, float y) throws IOException {
        cs.setStrokingColor(BORDER);
        cs.setLineWidth(0.5f);
        cs.moveTo(MARGIN, y);
        cs.lineTo(PAGE_W - MARGIN, y);
        cs.stroke();
    }

    private void fillRect(PDPageContentStream cs, float x, float y,
                           float w, float h, Color color) throws IOException {
        cs.setNonStrokingColor(color);
        cs.addRect(x, y, w, h);
        cs.fill();
    }

    private void drawText(PDPageContentStream cs, PDType1Font font,
                           float size, String text, float x, float y) throws IOException {
        if (text == null || text.isBlank()) return;
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        cs.showText(sanitise(text));
        cs.endText();
    }

    /** PDFBox built-in fonts only support Latin-1. Strip non-Latin chars safely. */
    private String sanitise(String text) {
        if (text == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            sb.append(c < 256 ? c : '?');
        }
        return sb.toString();
    }

    private float textWidth(PDType1Font font, float size, String text) throws IOException {
        return font.getStringWidth(sanitise(text)) / 1000f * size;
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max - 1) + "…" : s;
    }

    private String formatNum(double v) {
        return v == Math.floor(v) ? String.valueOf((int) v) : String.valueOf(v);
    }

    private Color gradeColor(String grade) {
        if (grade == null) return TEXT_MID;
        return switch (grade) {
            case "A+", "A" -> GRADE_GOOD;
            case "B", "C"  -> GRADE_WARN;
            default        -> GRADE_FAIL;
        };
    }
}
