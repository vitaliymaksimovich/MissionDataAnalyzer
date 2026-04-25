package service;

import parser.MissionParser;
import parser.plugin.ArchivePlugin;
import parser.plugin.ExportPlugin;
import parser.plugin.PluginMetadata;
import parser.plugin.ReportFormatterPlugin;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.prefs.Preferences;
import java.util.stream.Stream;

/**
 * Паттерн: Singleton + Plugin (расширяемость через плагины).
 *
 * Singleton: единственный экземпляр на всё приложение (INSTANCE).
 *   Гарантирует, что плагины загружаются ровно один раз и доступны из любого места.
 *
 * Plugin: динамическая загрузка расширений через URLClassLoader + ServiceLoader.
 *   Ядро приложения не знает о конкретных плагинах — только об интерфейсах
 *   (MissionParser, ExportPlugin, ReportFormatterPlugin).
 *   Новый тип плагина = JAR с META-INF/services/ в папке parser-plugins/.
 *   Ядро не перекомпилируется (OCP).
 */
public class PluginManager {

    private static final String PLUGINS_DIR_NAME = "parser-plugins";

    /** Preferences может быть null в headless/тестовой среде — обрабатываем безопасно */
    private static final Preferences PREFS = initPrefs();

    private static final PluginManager INSTANCE = new PluginManager();

    /** Безопасная инициализация Preferences — возвращает null при ошибке */
    private static Preferences initPrefs() {
        try {
            return Preferences.userRoot().node("mission_analyzer_plugins");
        } catch (Exception e) {
            return null;
        }
    }

    private final List<MissionParser>        parserPlugins  = new ArrayList<>();
    private final List<ExportPlugin>         exportPlugins  = new ArrayList<>();
    private final List<ReportFormatterPlugin> reportPlugins = new ArrayList<>();
    private final List<ArchivePlugin>        archivePlugins = new ArrayList<>();
    private final Set<String> disabledIds = new HashSet<>();
    private boolean loaded = false;
    private LoadResult lastResult;
    /** ClassLoader от предыдущей загрузки — закрывается при reload, чтобы не утекали дескрипторы JAR */
    private URLClassLoader currentClassLoader;

    /** Итог загрузки — для UI */
    public record LoadResult(
            boolean folderExists,
            int parsersFound,   // парсеры форматов + архивные распаковщики
            int exportsFound,
            int reportsFound,
            String error
    ) {}

    /** Информация об одном плагине — для таблицы в PluginsDialog */
    public record PluginInfo(PluginMetadata metadata, boolean builtin, boolean enabled) {}

    /** Единственный экземпляр менеджера плагинов */
    public static PluginManager getInstance() { return INSTANCE; }

    private PluginManager() {
        // Загружаем список отключённых плагинов из Preferences (если доступны)
        if (PREFS != null) {
            String saved = PREFS.get("disabled", "");
            if (!saved.isBlank()) {
                Arrays.stream(saved.split(","))
                        .filter(s -> !s.isBlank())
                        .forEach(disabledIds::add);
            }
        }
    }

    /** Загружает плагины один раз за сессию (кэш) */
    public synchronized void loadIfNeeded() {
        if (loaded) return;
        doLoad();
    }

    /** Горячая перезагрузка — очищает кэш и сканирует папку заново */
    public synchronized LoadResult reload() {
        loaded = false;
        parserPlugins.clear();
        exportPlugins.clear();
        reportPlugins.clear();
        archivePlugins.clear();
        closeClassLoader(); // закрываем старый ClassLoader перед новой загрузкой
        doLoad();
        return lastResult;
    }

    /** Закрывает текущий ClassLoader — освобождает дескрипторы JAR-файлов */
    private void closeClassLoader() {
        if (currentClassLoader != null) {
            try {
                currentClassLoader.close();
            } catch (Exception e) {
                AppLogger.error("Плагины: ошибка закрытия ClassLoader — " + e.getMessage());
            }
            currentClassLoader = null;
        }
    }

    /** Внутренняя загрузка плагинов через ServiceLoader */
    private void doLoad() {
        Path dir = findPluginsDir();
        if (dir == null) {
            // Выводим рабочую директорию, чтобы пользователь понял, где искали
            AppLogger.log("Плагины: папка " + PLUGINS_DIR_NAME + "/ не найдена"
                    + " (рабочая директория: " + Path.of("").toAbsolutePath() + ")");
            lastResult = new LoadResult(false, 0, 0, 0, null);
            loaded = true;
            return;
        }

        AppLogger.log("Плагины: папка найдена → " + dir.toAbsolutePath());

        int parsers = 0, exports = 0, reports = 0, archives = 0;
        String error = null;

        try {
            URL[] jars = scanJars(dir);
            if (jars.length == 0) {
                AppLogger.log("Плагины: папка пуста (JAR-файлы не найдены)");
            } else {
                AppLogger.log("Плагины: найдено JAR-файлов: " + jars.length);
                URLClassLoader cl = new URLClassLoader(jars, MissionParser.class.getClassLoader());
                currentClassLoader = cl; // сохраняем, чтобы закрыть при reload

                // Загружаем парсеры через ServiceLoader
                ServiceLoader.load(MissionParser.class, cl).forEach(p -> {
                    parserPlugins.add(p);
                    AppLogger.log("Плагин-парсер: " + p.getFormatName());
                });
                parsers = parserPlugins.size();

                // Загружаем плагины экспорта через ServiceLoader
                ServiceLoader.load(ExportPlugin.class, cl).forEach(e -> {
                    exportPlugins.add(e);
                    AppLogger.log("Плагин-экспорт: " + e.getMetadata().name());
                });
                exports = exportPlugins.size();

                // Загружаем плагины форматтеров отчётов через ServiceLoader
                ServiceLoader.load(ReportFormatterPlugin.class, cl).forEach(r -> {
                    reportPlugins.add(r);
                    AppLogger.log("Плагин-отчёт: " + r.getDisplayName());
                });
                reports = reportPlugins.size();

                // Загружаем архивные плагины через ServiceLoader
                ServiceLoader.load(ArchivePlugin.class, cl).forEach(a -> {
                    archivePlugins.add(a);
                    AppLogger.log("Плагин-архив: " + a.getMetadata().name());
                });
                archives = archivePlugins.size();

                int totalParsers = parsers + archives;
                if (totalParsers + exports + reports > 0) {
                    AppLogger.log("Плагины: всего " + (totalParsers + exports + reports)
                            + " (" + totalParsers + " парс., " + exports + " экспорт., "
                            + reports + " отчёт.)");
                }
            }
        } catch (Throwable e) {
            // Throwable вместо Exception: ServiceConfigurationError extends Error, не Exception
            error = e.getClass().getSimpleName() + ": " + e.getMessage();
            AppLogger.error("Плагины: ошибка загрузки — " + error);
        }

        // parsersFound включает и архивные плагины — для пользователя это «парсеры»
        lastResult = new LoadResult(true, parsers + archives, exports, reports, error);
        loaded = true;
    }

    // --- Геттеры ---

    /** Все плагины-парсеры (без встроенных), независимо от enabled */
    public List<MissionParser> getAllParserPlugins() {
        return Collections.unmodifiableList(parserPlugins);
    }

    /** Только включённые плагины-парсеры */
    public List<MissionParser> getActiveParserPlugins() {
        return parserPlugins.stream()
                .filter(p -> isEnabled(p.getMetadata().id()))
                .toList();
    }

    /** Все плагины экспорта, независимо от enabled */
    public List<ExportPlugin> getAllExportPlugins() {
        return Collections.unmodifiableList(exportPlugins);
    }

    /** Только включённые плагины экспорта */
    public List<ExportPlugin> getActiveExportPlugins() {
        return exportPlugins.stream()
                .filter(p -> isEnabled(p.getMetadata().id()))
                .toList();
    }

    /** Все плагины-форматтеры отчётов, независимо от enabled */
    public List<ReportFormatterPlugin> getAllReportPlugins() {
        return Collections.unmodifiableList(reportPlugins);
    }

    /** Только включённые плагины-форматтеры отчётов */
    public List<ReportFormatterPlugin> getActiveReportPlugins() {
        return reportPlugins.stream()
                .filter(p -> isEnabled(p.getMetadata().id()))
                .toList();
    }

    /** Все архивные плагины, независимо от enabled */
    public List<ArchivePlugin> getAllArchivePlugins() {
        return Collections.unmodifiableList(archivePlugins);
    }

    /** Только включённые архивные плагины */
    public List<ArchivePlugin> getActiveArchivePlugins() {
        return archivePlugins.stream()
                .filter(p -> isEnabled(p.getMetadata().id()))
                .toList();
    }

    /** Итог последней загрузки — для UI (null до первого вызова loadIfNeeded) */
    public LoadResult getLastResult() { return lastResult; }

    // --- Включение / отключение ---

    /** Проверяет, включён ли плагин с данным id */
    public boolean isEnabled(String pluginId) {
        return !disabledIds.contains(pluginId);
    }

    /** Включает или отключает плагин, сохраняет состояние в Preferences */
    public void setEnabled(String pluginId, boolean enabled) {
        if (enabled) {
            disabledIds.remove(pluginId);
        } else {
            disabledIds.add(pluginId);
        }
        // Сохраняем только если Preferences доступны
        if (PREFS != null) {
            PREFS.put("disabled", String.join(",", disabledIds));
        }
        AppLogger.log("Плагин '" + pluginId + "' " + (enabled ? "включён" : "отключён"));
    }

    // --- Поиск папки плагинов ---

    /**
     * Ищет папку parser-plugins:
     * 1. Текущая рабочая директория
     * 2. Корень Maven-модуля (pom.xml выше target/classes)
     */
    private static Path findPluginsDir() {
        // Собираем кандидатов в порядке приоритета
        List<Path> candidates = new ArrayList<>();

        try {
            Path classLoc = Path.of(PluginManager.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI());
            Path base = Files.isDirectory(classLoc) ? classLoc : classLoc.getParent();
            if (base != null) {
                // 1. Корень Maven-модуля (pom.xml ближайший снизу) — самый надёжный
                Path moduleRoot = findMavenRoot(base);
                if (moduleRoot != null)
                    candidates.add(moduleRoot.resolve(PLUGINS_DIR_NAME));

                // 2. Рядом с классами / JAR-ом
                candidates.add(base.resolve(PLUGINS_DIR_NAME));
            }
        } catch (Exception ignored) {}

        // 3. Рабочая директория
        candidates.add(Path.of(PLUGINS_DIR_NAME));

        // Сначала ищем папку с реальными JAR-файлами (пустые пропускаем)
        for (Path p : candidates) {
            if (Files.isDirectory(p) && hasJars(p)) return p;
        }

        // Если JAR нет нигде — возвращаем первую существующую (покажем "папка пуста")
        for (Path p : candidates) {
            if (Files.isDirectory(p)) return p;
        }

        return null;
    }

    /** Есть ли в директории хотя бы один JAR-файл */
    private static boolean hasJars(Path dir) {
        try (var s = Files.list(dir)) {
            return s.anyMatch(p -> p.toString().toLowerCase().endsWith(".jar"));
        } catch (Exception e) {
            return false;
        }
    }

    /** Поднимается вверх до первой папки с pom.xml */
    private static Path findMavenRoot(Path start) {
        Path cur = start;
        while (cur != null) {
            if (Files.exists(cur.resolve("pom.xml"))) return cur;
            cur = cur.getParent();
        }
        return null;
    }

    /** Сканирует директорию и возвращает URL всех JAR-файлов */
    private static URL[] scanJars(Path dir) throws Exception {
        try (Stream<Path> s = Files.list(dir)) {
            return s.filter(p -> p.toString().toLowerCase().endsWith(".jar"))
                    .map(p -> {
                        try { return p.toUri().toURL(); }
                        catch (Exception e) { return null; }
                    })
                    .filter(Objects::nonNull)
                    .toArray(URL[]::new);
        }
    }
}
