package parser;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class ParserFactory {

    private static final Map<String, Supplier<MissionParser>> PARSERS = new HashMap<>();

    static {
        PARSERS.put("txt", TxtMissionParser::new);
        PARSERS.put("json", JsonMissionParser::new);
        PARSERS.put("xml", XmlMissionParser::new);
        PARSERS.put("html", XmlMissionParser::new);
    }

    private ParserFactory() {
    }

    public static MissionParser createParser(File file) {
        validateFile(file);

        String extension = getFileExtension(file.getName()).toLowerCase();
        Supplier<MissionParser> parserSupplier = PARSERS.get(extension);

        if (parserSupplier == null) {
            throw new IllegalArgumentException("Unsupported file format: " + extension);
        }

        return parserSupplier.get();
    }

    private static void validateFile(File file) {
        if (file == null) {
            throw new IllegalArgumentException("File must not be null.");
        }
    }

    private static String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');

        if (dotIndex == -1 || dotIndex == fileName.length() - 1) {
            throw new IllegalArgumentException("File has no extension: " + fileName);
        }

        return fileName.substring(dotIndex + 1);
    }
}