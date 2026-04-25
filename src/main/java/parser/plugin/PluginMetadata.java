package parser.plugin;

/**
 * Метаданные плагина — версия, автор, описание.
 * Используется в PluginsDialog для отображения списка плагинов.
 */
public record PluginMetadata(
        PluginType type,
        String id,          // уникальный идентификатор: "csv-parser", "markdown-export"
        String name,        // отображаемое имя: "CSV Parser"
        String version,
        String author,
        String description,
        String formatHint   // для PARSER: расширение ("csv"), для EXPORT: имя кнопки ("Markdown")
) {}
