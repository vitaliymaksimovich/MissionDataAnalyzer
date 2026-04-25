package parser;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class ParserRegistry {

    private final Map<String, Supplier<MissionParser>> registry = new HashMap<>();

    public void register(String extension, Supplier<MissionParser> supplier) {
        registry.put(extension.toLowerCase(), supplier);
    }

    public MissionParser getParser(File file) throws IOException {
        String name = file.getName();
        int dot = name.lastIndexOf('.');
        String ext = (dot == -1 || dot == name.length() - 1)
                ? FormatDetector.detect(file)
                : name.substring(dot + 1).toLowerCase();

        return getParser(ext);
    }

    /** Получение парсера по имени формата (для REST API / InputStream, когда файла нет) */
    public MissionParser getParser(String format) {
        Supplier<MissionParser> supplier = registry.get(format.toLowerCase());
        if (supplier == null) {
            throw new IllegalArgumentException("Неизвестный формат: " + format);
        }
        return supplier.get();
    }
}