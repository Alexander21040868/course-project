package org.example.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.example.dto.LessonProgressDto;
import org.example.dto.StudentDetailDto;
import org.example.dto.TaskProgressDto;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
public class PdfExportService {

    private final StudentProgressService progressService;

    public PdfExportService(StudentProgressService progressService) {
        this.progressService = progressService;
    }

    public byte[] exportStudentReport(Long userId) {
        StudentDetailDto d = progressService.getDetail(userId);
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            float y = page.getMediaBox().getHeight() - 50;
            float left = 50;
            float line = 14;

            PDPageContentStream cs = new PDPageContentStream(doc, page);
            y = writeLine(cs, font, 16, left, y, "CodeQuest / Progress report");
            y -= line;
            y = writeLine(cs, font, 12, left, y, "User: " + ascii(d.username()));
            y = writeLine(cs, font, 12, left, y, "XP: " + d.xp() + "  Level: " + d.level() + "  Solved: " + d.totalSolved());
            y -= line;

            for (LessonProgressDto lp : d.lessons()) {
                if (y < 72) {
                    cs.close();
                    page = new PDPage(PDRectangle.A4);
                    doc.addPage(page);
                    cs = new PDPageContentStream(doc, page);
                    y = page.getMediaBox().getHeight() - 50;
                }
                y = writeLine(cs, font, 11, left, y, "Lesson: " + ascii(lp.title()) + " (" + lp.solved() + "/" + lp.total() + ")");
                for (TaskProgressDto t : lp.tasks()) {
                    if (y < 56) {
                        cs.close();
                        page = new PDPage(PDRectangle.A4);
                        doc.addPage(page);
                        cs = new PDPageContentStream(doc, page);
                        y = page.getMediaBox().getHeight() - 50;
                    }
                    String mark = t.solved() ? "[+]" : "[ ]";
                    y = writeLine(cs, font, 10, left + 12, y,
                            mark + " " + ascii(t.title()) + "  " + t.difficulty() + "  att:" + t.attempts());
                }
                y -= line / 2;
            }
            cs.close();
            doc.save(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("PDF error", e);
        }
    }

    private static float writeLine(PDPageContentStream cs, PDType1Font font, float size, float x, float y, String text)
            throws IOException {
        String t = text.length() > 110 ? text.substring(0, 107) + "..." : text;
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        cs.showText(t);
        cs.endText();
        return y - size - 4;
    }

    private static String ascii(String s) {
        if (s == null) return "";
        StringBuilder b = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (c >= 32 && c < 127) b.append(c);
            else b.append('?');
        }
        return b.toString();
    }
}
