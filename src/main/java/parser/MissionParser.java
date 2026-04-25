package parser;

import model.Mission;
import parser.plugin.PluginMetadata;
import parser.plugin.PluginType;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Set;

public abstract class MissionParser {

    /** Поля, обрабатываемые напрямую каждым парсером (не делегируются в BlockParserChain) */
    protected static final Set<String> CORE_FIELDS = Set.of(
            "missionId", "date", "location", "outcome", "damageCost",
            "curse", "sorcerers", "techniques", "comment"
    );

    public abstract Mission parse(File file) throws IOException;

    /**
     * Имя формата для логов и UI. Встроенные парсеры — простое имя класса,
     * плагины переопределяют (например "JSON", "Pipe").
     */
    public String getFormatName() {
        return getClass().getSimpleName();
    }

    /**
     * Метаданные для плагинов (имя, версия, автор) — отображаются в PluginsDialog.
     * Default-заглушка на основе getFormatName(), чтобы старые плагины,
     * собранные до появления этого метода, не падали с NPE.
     * Свежие плагины переопределяют и возвращают полную информацию.
     */
    public PluginMetadata getMetadata() {
        String fmt = getFormatName();
        return new PluginMetadata(
                PluginType.PARSER,
                fmt.toLowerCase(),
                fmt,
                "—",
                "—",
                "Парсер формата " + fmt,
                fmt.toLowerCase()
        );
    }

    /**
     * Парсинг из потока данных (REST API, БД, тесты).
     * По умолчанию сохраняет во временный файл — подклассы могут переопределить для эффективности.
     */
    public Mission parse(InputStream in) throws IOException {
        Path tmp = Files.createTempFile("mission-", ".tmp");
        try {
            Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
            return parse(tmp.toFile());
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

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