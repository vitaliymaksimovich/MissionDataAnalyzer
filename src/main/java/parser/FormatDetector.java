package parser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class FormatDetector {

    private FormatDetector() {}

    /** Определение формата по содержимому файла */
    public static String detect(File file) throws IOException {
        String content = Files.readString(file.toPath());
        return detect(content);
    }

    /** Определение формата по строковому содержимому (для REST API / InputStream) */
    public static String detect(String content) {
        for (String line : content.lines().toList()) {
            String t = line.trim();
            if (t.isEmpty()) continue;
            if (t.startsWith("{"))                    return "json";
            if (t.startsWith("<"))                    return "xml";
            if (t.contains("|"))                      return "pipe";
            if (t.startsWith("[") && t.endsWith("]")) return "txt";
            if (t.matches("\\w+:.*") && !t.contains(": ")) return "yaml"; // "key:" без пробела — YAML
            if (t.contains(": "))                     return "txt";  // "key: value" — наш txt формат
        }
        return "txt";
    }
}