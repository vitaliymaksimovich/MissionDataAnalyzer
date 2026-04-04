package parser;

import model.Mission;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public abstract class MissionParser {

    public abstract Mission parse(File file) throws IOException;

    protected String readFile(File file) throws IOException {
        validate(file);
        return Files.readString(file.toPath());
    }

    protected void validate(File file) {
        if (file == null) {
            throw new IllegalArgumentException("Файл не указан.");
        }
        if (!file.exists()) {
            throw new IllegalArgumentException("Файл не найден: " + file.getAbsolutePath());
        }
        if (!file.isFile()) {
            throw new IllegalArgumentException("Указанный путь не является файлом: " + file.getAbsolutePath());
        }
        if (!file.canRead()) {
            throw new IllegalArgumentException("Нет прав на чтение файла: " + file.getAbsolutePath());
        }
    }
}