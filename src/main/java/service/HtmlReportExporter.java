package service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class HtmlReportExporter {

    private static final String TEMPLATE_PATH = "/templates/report-template.html";

    public void export(File outputFile, String reportText) throws IOException {
        String template = loadResourceAsString(TEMPLATE_PATH);
        String html = template.replace("${reportText}", escapeHtml(reportText));
        write(outputFile, html);
    }

    public void exportAiResult(File outputFile, String missionId,
                               String aiTitle, String reportText,
                               String aiText) throws IOException {
        String html = """
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="UTF-8">
              <title>GigaChat — %s</title>
              <style>
                @page { margin: 20mm; }
                * { box-sizing: border-box; }
                html, body {
                  margin: 0; padding: 0;
                  font-family: "Palatino Linotype", Georgia, serif;
                  background: #e8e0d0;
                }
                body {
                  display: flex;
                  justify-content: center;
                  align-items: flex-start;
                  padding: 40px 0 60px 0;
                }
                .page {
                  width: 210mm;
                  background: #f5f0e8;
                  box-shadow: 0 0 24px rgba(0,0,0,0.25);
                  border: 1px solid #c8b89a;
                }
                .header {
                  text-align: center;
                  padding: 32px 40px 20px 40px;
                  border-bottom: 2px solid #8a7a5a;
                }
                .header-title {
                  font-size: 22px;
                  font-weight: 700;
                  color: #3a4a2e;
                  letter-spacing: 2px;
                  text-transform: uppercase;
                  margin-bottom: 6px;
                }
                .header-subtitle {
                  font-size: 13px;
                  color: #7a6a4a;
                  letter-spacing: 1px;
                }
                .divider {
                  text-align: center;
                  color: #8a7a5a;
                  font-size: 18px;
                  padding: 6px 0;
                  letter-spacing: 8px;
                }
                .section-title {
                  font-size: 13px;
                  font-weight: 700;
                  letter-spacing: 1px;
                  text-transform: uppercase;
                  color: #5a6a3a;
                  border-bottom: 1px solid #c8b89a;
                  margin: 0 44px 12px 44px;
                  padding-bottom: 4px;
                }
                .report-body {
                  padding: 20px 44px;
                  color: #2e3a2e;
                  font-size: 15px;
                  line-height: 1.6;
                  font-weight: 500;
                  white-space: pre-wrap;
                  overflow-wrap: anywhere;
                  word-break: break-word;
                }
                .ai-body {
                  padding: 20px 44px 36px 44px;
                  color: #2e3a2e;
                  font-size: 15px;
                  line-height: 1.6;
                  font-weight: 500;
                  white-space: pre-wrap;
                  overflow-wrap: anywhere;
                  word-break: break-word;
                }
                .footer {
                  border-top: 1px solid #c8b89a;
                  padding: 12px 44px;
                  text-align: right;
                  font-size: 11px;
                  color: #9a8a6a;
                  font-style: italic;
                }
                @media print {
                  html, body { background: white; padding: 0; }
                  body { display: block; }
                  .page { box-shadow: none; border: none; width: 100%%; }
                }
              </style>
            </head>
            <body>
            <div class="page">
              <div class="header">
                <div class="header-title">Отчёт о миссии</div>
                <div class="header-subtitle">%s — %s</div>
              </div>
              <div class="divider">✦ ✦ ✦</div>
              <div class="section-title">Данные миссии</div>
              <div class="report-body">%s</div>
              <div class="divider">✦ ✦ ✦</div>
              <div class="section-title">GigaChat — %s</div>
              <div class="ai-body">%s</div>
              <div class="footer">Документ сформирован системой Mission Analyzer</div>
            </div>
            </body>
            </html>
            """.formatted(
                aiTitle,
                missionId, aiTitle,
                escapeHtml(reportText),
                aiTitle,
                escapeHtml(aiText)
        );

        write(outputFile, html);
    }

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

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}