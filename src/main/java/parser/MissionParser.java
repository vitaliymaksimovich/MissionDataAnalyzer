package parser;

import model.Mission;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public abstract class MissionParser {

    public abstract Mission parse(File file) throws IOException;

    protected void validateFile(File file) {
        if (file == null) {
            throw new IllegalArgumentException("File must not be null.");
        }
        if (!file.exists()) {
            throw new IllegalArgumentException("File does not exist: " + file.getAbsolutePath());
        }
        if (!file.isFile()) {
            throw new IllegalArgumentException("Path is not a file: " + file.getAbsolutePath());
        }
    }

    protected String readFileContent(File file) throws IOException {
        validateFile(file);
        return Files.readString(file.toPath());
    }
}
