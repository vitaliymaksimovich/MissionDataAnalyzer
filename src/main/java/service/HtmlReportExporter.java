package service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class HtmlReportExporter {

    private static final String TEMPLATE_PATH = "/templates/report-template.html";
    private static final String BACKGROUND_IMAGE_PATH = "/templates/assets/report-background.png";

    public void export(File outputFile, String reportText, String aiReviewText) throws IOException {
        String template = loadResourceAsString(TEMPLATE_PATH);
        String backgroundImageBase64 = loadResourceAsBase64(BACKGROUND_IMAGE_PATH);

        String html = template
                .replace("${reportText}", escapeHtml(reportText))
                .replace("${aiReviewText}", escapeHtml(aiReviewText))
                .replace("${backgroundImage}", backgroundImageBase64);

        try (FileWriter writer = new FileWriter(outputFile, StandardCharsets.UTF_8)) {
            writer.write(html);
        }
    }

    private String loadResourceAsString(String path) throws IOException {
        try (InputStream inputStream = getClass().getResourceAsStream(path)) {
            if (inputStream == null) {
                throw new IllegalStateException("Ресурс не найден: " + path);
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private String loadResourceAsBase64(String path) throws IOException {
        try (InputStream inputStream = getClass().getResourceAsStream(path)) {
            if (inputStream == null) {
                throw new IllegalStateException("Изображение не найдено: " + path);
            }
            byte[] bytes = inputStream.readAllBytes();
            return Base64.getEncoder().encodeToString(bytes);
        }
    }

    private String escapeHtml(String text) {
        if (text == null) {
            return "";
        }

        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
