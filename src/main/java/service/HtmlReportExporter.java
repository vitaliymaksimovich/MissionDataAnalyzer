package service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class HtmlReportExporter {

    private static final String TEMPLATE_PATH       = "/templates/report-template.html";
    private static final String BACKGROUND_IMAGE_PATH = "/templates/assets/report-background.png";

    // --- экспорт отчёта миссии (через шаблон с фоном) ---
    public void export(File outputFile, String reportText) throws IOException {
        String template = loadResourceAsString(TEMPLATE_PATH);
        String backgroundBase64 = loadResourceAsBase64(BACKGROUND_IMAGE_PATH);

        String html = template
                .replace("${reportText}", escapeHtml(reportText))
                .replace("${backgroundImage}", backgroundBase64);

        write(outputFile, html);
    }

    // --- экспорт AI-результата (простой чистый HTML, без шаблона) ---
    public void exportAiResult(File outputFile, String missionId,
                               String aiTitle, String reportText,
                               String aiText) throws IOException {
        String backgroundBase64 = loadResourceAsBase64(BACKGROUND_IMAGE_PATH);

        String html = """
        <!DOCTYPE html>
        <html>
        <head>
          <meta charset="UTF-8">
          <title>GigaChat — %s</title>
          <style>
            @page { size: A4 portrait; margin: 0; }
            * { box-sizing: border-box; }
            html, body {
              margin: 0; padding: 0;
              background: #bfbfbf;
              font-family: "Palatino Linotype", Georgia, serif;
            }
            body {
              display: flex;
              justify-content: center;
              align-items: flex-start;
              padding: 20px 0;
            }
            .page {
              position: relative;
              width: 210mm;
              min-height: 297mm;
              background-image: url("data:image/png;base64,%s");
              background-size: cover;
              background-repeat: no-repeat;
              background-position: center;
              box-shadow: 0 0 18px rgba(0,0,0,0.25);
              color: #2e3f2e;
              padding: 60mm 11%% 20mm 11%%;
              -webkit-print-color-adjust: exact;
              print-color-adjust: exact;
            }
            .section-title {
              font-size: 11px;
              font-weight: 700;
              letter-spacing: 1px;
              text-transform: uppercase;
              border-bottom: 1px solid #5a6e5a;
              margin: 16px 0 6px 0;
              padding-bottom: 3px;
            }
            .report-text {
              font-size: 10px;
              line-height: 1.4;
              font-weight: 600;
              white-space: pre-wrap;
              overflow-wrap: anywhere;
              word-break: break-word;
              text-shadow: 0 0 2px rgba(0,0,0,0.1);
            }
            .ai-text {
              font-size: 11px;
              line-height: 1.5;
              font-weight: 600;
              white-space: pre-wrap;
              overflow-wrap: anywhere;
              word-break: break-word;
              text-shadow: 0 0 2px rgba(0,0,0,0.1);
              margin-top: 8px;
            }
            @media print {
              html, body { background: white; padding: 0; }
              body { display: block; }
              .page { box-shadow: none; }
            }
          </style>
        </head>
        <body>
        <div class="page">
          <div class="section-title">Отчёт о миссии</div>
          <div class="report-text">%s</div>
          <div class="section-title">GigaChat — %s</div>
          <div class="ai-text">%s</div>
        </div>
        </body>
        </html>
        """.formatted(
                aiTitle,
                backgroundBase64,
                escapeHtml(reportText),
                aiTitle,
                escapeHtml(aiText)
        );

        write(outputFile, html);
    }

    // --- утилиты ---

    private void write(File outputFile, String html) throws IOException {
        try (FileWriter writer = new FileWriter(outputFile, StandardCharsets.UTF_8)) {
            writer.write(html);
        }
    }

    private String loadResourceAsString(String path) throws IOException {
        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is == null) throw new IllegalStateException("Ресурс не найден: " + path);
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private String loadResourceAsBase64(String path) throws IOException {
        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is == null) throw new IllegalStateException("Изображение не найдено: " + path);
            return Base64.getEncoder().encodeToString(is.readAllBytes());
        }
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}