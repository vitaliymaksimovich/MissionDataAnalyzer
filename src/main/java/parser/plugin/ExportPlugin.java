package parser.plugin;

import java.io.File;
import java.io.IOException;

/**
 * Паттерн: Strategy — точка расширения для экспорта (Plugin Extension Point).
 *
 * Определяет единый интерфейс для всех алгоритмов сохранения отчёта в файл.
 * Клиент (MainFrame) работает только с этим интерфейсом — конкретный формат
 * (Markdown, CSV, PDF и т.д.) подставляется в рантайме из плагина.
 *
 * Подключение нового формата экспорта:
 *   1. Реализовать этот интерфейс в отдельном JAR
 *   2. Добавить META-INF/services/parser.plugin.ExportPlugin с именем класса
 *   3. Положить JAR в parser-plugins/ — новый пункт появится в меню «Экспорт»
 */
public interface ExportPlugin {

    /** Метаданные плагина — имя, версия, автор */
    PluginMetadata getMetadata();

    /** Сохраняет содержимое content в указанный файл */
    void export(String content, File outputFile) throws IOException;

    /** Расширение выходного файла (по умолчанию .txt) */
    default String getFileExtension() { return ".txt"; }
}
