package parser;

import service.AppLogger;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.stream.Stream;

/**
 * Загрузка плагинов-парсеров через ServiceLoader — OCP без перекомпиляции.
 * Новый формат = JAR-файл в папке parser-plugins/ с META-INF/services/parser.MissionParser.
 */
public class PluginLoader {

    private static final String PLUGINS_DIR_NAME = "parser-plugins";

    // Кэш — ServiceLoader вызывается ровно один раз за сессию
    private static boolean loaded = false;
    private static List<MissionParser> cachedPlugins = List.of();
    private static LoadResult cachedResult;

    /** Итог загрузки плагинов — для отображения в UI */
    public record LoadResult(boolean folderExists, List<String> loadedFormats, String errorMessage) {}

    public static synchronized List<MissionParser> loadPlugins() {
        if (loaded) return cachedPlugins;
        loaded = true;

        List<MissionParser> parsers = new ArrayList<>();
        List<String> formats = new ArrayList<>();
        String error = null;

        Path pluginsDir = findPluginsDir();
        if (pluginsDir == null) {
            AppLogger.log("Плагины: папка " + PLUGINS_DIR_NAME + "/ не найдена");
            cachedResult = new LoadResult(false, formats, null);
            cachedPlugins = parsers;
            return cachedPlugins;
        }

        try {
            URL[] jars = findJarUrls(pluginsDir);
            if (jars.length == 0) {
                AppLogger.log("Плагины: папка " + PLUGINS_DIR_NAME + "/ пуста");
            } else {
                URLClassLoader cl = new URLClassLoader(jars,
                        MissionParser.class.getClassLoader());

                ServiceLoader<MissionParser> loader =
                        ServiceLoader.load(MissionParser.class, cl);

                for (MissionParser parser : loader) {
                    parsers.add(parser);
                    formats.add(parser.getFormatName());
                    AppLogger.log("Плагин подключён: " + parser.getFormatName());
                }

                if (!formats.isEmpty()) {
                    AppLogger.log("Плагины: всего подключено " + formats.size()
                            + " (" + String.join(", ", formats) + ")");
                }
            }
        } catch (Exception e) {
            error = e.getMessage();
            AppLogger.error("Плагины: ошибка загрузки — " + error);
        }

        cachedResult = new LoadResult(true, formats, error);
        cachedPlugins = parsers;
        return cachedPlugins;
    }

    public static LoadResult getLoadResult() {
        return cachedResult;
    }

    /**
     * Ищет папку parser-plugins в трёх стандартных местах:
     * 1. Рабочая директория (стандартный запуск из IDE/Maven).
     * 2. Корень Maven-модуля, где лежит pom.xml (для запуска, когда IDE открыла родительский проект).
     * 3. Рядом с JAR-ом программы (запуск через java -jar).
     * Возвращает null, если папка не найдена ни в одном из мест.
     */
    private static Path findPluginsDir() {
        Path current = Path.of(PLUGINS_DIR_NAME);
        if (Files.isDirectory(current)) return current;

        try {
            Path classLocation = Path.of(PluginLoader.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI());
            Path baseDir = Files.isDirectory(classLocation) ? classLocation : classLocation.getParent();

            if (baseDir != null) {
                // Корень Maven-модуля (там где pom.xml) — помогает при запуске, когда IDE
                // открыла родительскую папку выше нашего модуля
                Path moduleRoot = findMavenModuleRoot(baseDir);
                if (moduleRoot != null) {
                    Path inModule = moduleRoot.resolve(PLUGINS_DIR_NAME);
                    if (Files.isDirectory(inModule)) return inModule;
                }

                // Прямо рядом с JAR
                Path nearJar = baseDir.resolve(PLUGINS_DIR_NAME);
                if (Files.isDirectory(nearJar)) return nearJar;
            }
        } catch (Exception ignored) {}

        return null;
    }

    /** Поднимается вверх от start до первой папки с pom.xml */
    private static Path findMavenModuleRoot(Path start) {
        Path cur = start;
        while (cur != null) {
            if (Files.exists(cur.resolve("pom.xml"))) return cur;
            cur = cur.getParent();
        }
        return null;
    }

    private static URL[] findJarUrls(Path dir) throws Exception {
        try (Stream<Path> stream = Files.list(dir)) {
            return stream
                    .filter(p -> p.toString().toLowerCase().endsWith(".jar"))
                    .map(PluginLoader::toUrl)
                    .filter(Objects::nonNull)
                    .toArray(URL[]::new);
        }
    }

    private static URL toUrl(Path p) {
        try { return p.toUri().toURL(); }
        catch (Exception e) { return null; }
    }

    private PluginLoader() {}
}
