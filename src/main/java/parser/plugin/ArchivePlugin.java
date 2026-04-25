package parser.plugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Паттерн: Strategy — точка расширения для архивных форматов (Plugin Extension Point).
 *
 * Распаковывает архив в временную директорию и возвращает список извлечённых файлов.
 * MissionLoader затем обрабатывает каждый файл через обычный конвейер парсинга —
 * те же проверки форматов, дубликатов и ошибок, что и при загрузке обычных файлов.
 *
 * Подключение нового архивного формата:
 *   1. Реализовать этот интерфейс в отдельном JAR
 *   2. Добавить META-INF/services/parser.plugin.ArchivePlugin с именем класса
 *   3. Положить JAR в parser-plugins/
 */
public interface ArchivePlugin {

    /** Возвращает true если этот плагин умеет распаковывать данный файл */
    boolean canHandle(File file);

    /**
     * Извлекает содержимое архива в tempDir.
     * Возвращает список файлов, готовых к парсингу (без директорий и системных файлов).
     */
    List<File> extract(File archive, Path tempDir) throws IOException;

    /** Метаданные плагина — для отображения в PluginsDialog и тосте */
    PluginMetadata getMetadata();
}
