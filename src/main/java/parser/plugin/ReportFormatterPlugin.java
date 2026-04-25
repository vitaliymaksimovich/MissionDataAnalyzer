package parser.plugin;

import service.ReportFormatter;

/**
 * Паттерн: Strategy — точка расширения для форматтеров отчётов (Plugin Extension Point).
 *
 * Расширяет интерфейс Strategy (ReportFormatter) метаданными плагина.
 * Реализации автоматически регистрируются в выпадающем списке «Тип отчёта»
 * через ReportFormatterRegistryConfig — без изменения ядра.
 *
 * Подключение нового типа отчёта:
 *   1. Реализовать этот интерфейс в отдельном JAR
 *   2. Добавить META-INF/services/parser.plugin.ReportFormatterPlugin с именем класса
 *   3. Положить JAR в parser-plugins/ — новый пункт появится в комбобоксе «Тип отчёта»
 */
public interface ReportFormatterPlugin extends ReportFormatter {

    /** Метаданные плагина — для PluginsDialog и MarketplaceDialog */
    PluginMetadata getMetadata();
}
