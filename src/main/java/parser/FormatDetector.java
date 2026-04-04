package parser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

public class FormatDetector {

    private FormatDetector() {}

    public static String detect(File file) throws IOException {
        List<String> lines = Files.readAllLines(file.toPath());
        for (String line : lines) {
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