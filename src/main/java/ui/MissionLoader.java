package ui;

import model.Mission;
import parser.plugin.ArchivePlugin;
import service.AppLogger;
import service.MissionService;
import service.PluginManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Загрузчик миссий — изолирует всю I/O логику от UI.
 * MainFrame использует этот класс для загрузки из файлов, URL и директорий.
 * SRP: только загрузка, никакой компоновки UI.
 */
public class MissionLoader {

    /** Результат пакетной загрузки из файлов */
    public record FileLoadResult(
            List<Mission> loaded,
            List<File>    files,
            List<String>  errors,
            List<String>  duplicates
    ) {}

    /** Результат загрузки из директории (через SwingWorker) */
    public record DirectoryLoadResult(
            int          loaded,
            int          total,
            List<String> errors,
            List<String> duplicates
    ) {}

    private static final Font UI_FONT = new Font("Segoe UI", Font.PLAIN, 14);

    private final MissionService service;

    public MissionLoader(MissionService service) {
        this.service = service;
    }

    // -------------------------------------------------------------------------
    // Загрузка из файлов
    // -------------------------------------------------------------------------

    /**
     * Загружает миссии из массива файлов (результат JFileChooser).
     * Не показывает диалогов — возвращает результат, вызывающий код решает, что отобразить.
     */
    public FileLoadResult loadFiles(File[] selectedFiles, Set<String> loadedKeys) {
        List<Mission> loaded     = new ArrayList<>();
        List<File>    files      = new ArrayList<>();
        List<String>  errors     = new ArrayList<>();
        List<String>  duplicates = new ArrayList<>();
        List<Path>    tempDirs   = new ArrayList<>();

        // Шаг 1: расширяем архивы — каждый ZIP разворачивается в список обычных файлов.
        // Остальные файлы проходят без изменений. Порядок сохраняется.
        List<File> expandedFiles = new ArrayList<>();
        for (File file : selectedFiles) {
            ArchivePlugin archiver = findArchiver(file);
            if (archiver != null) {
                try {
                    Path tempDir = Files.createTempDirectory("mission-archive-");
                    tempDirs.add(tempDir);
                    List<File> extracted = archiver.extract(file, tempDir);
                    if (extracted.isEmpty()) {
                        errors.add(file.getName() + ": архив пуст или не содержит файлов миссий");
                    } else {
                        AppLogger.log("Архив " + file.getName() + ": распаковано " + extracted.size() + " файлов");
                        expandedFiles.addAll(extracted);
                    }
                } catch (IOException e) {
                    AppLogger.error("Архив " + file.getName() + ": ошибка распаковки — " + e.getMessage());
                    errors.add(file.getName() + ": ошибка распаковки — " + e.getMessage());
                }
            } else {
                expandedFiles.add(file);
            }
        }

        // Шаг 2: обрабатываем каждый файл — та же логика, что и без архивов
        for (File file : expandedFiles) {
            try {
                Mission mission = service.loadMission(file);

                String fname = file.getName();
                int dot = fname.lastIndexOf('.');
                String ext = dot >= 0 ? fname.substring(dot + 1).toLowerCase() : "";
                String key = fname + ":" + ext;

                if (!loadedKeys.add(key)) {
                    duplicates.add(fname);
                    continue;
                }

                loaded.add(mission);
                files.add(file);
            } catch (Exception e) {
                AppLogger.error("Не удалось загрузить файл: " + file.getName() + " — " + e.getMessage());
                errors.add(file.getName() + ": " + e.getMessage());
            }
        }

        // Шаг 3: удаляем временные директории с распакованными файлами
        for (Path tempDir : tempDirs) {
            try {
                Files.walk(tempDir)
                        .sorted(Comparator.reverseOrder())
                        .forEach(p -> { try { Files.delete(p); } catch (IOException ignored) {} });
            } catch (IOException ignored) {}
        }

        return new FileLoadResult(loaded, files, errors, duplicates);
    }

    /** Возвращает архивный плагин, умеющий обработать этот файл, или null */
    private ArchivePlugin findArchiver(File file) {
        return PluginManager.getInstance().getActiveArchivePlugins().stream()
                .filter(a -> a.canHandle(file))
                .findFirst()
                .orElse(null);
    }

    // -------------------------------------------------------------------------
    // Загрузка из одного URL
    // -------------------------------------------------------------------------

    /**
     * Загружает одну миссию из URL.
     * Бросает исключение при ошибке — вызывающий код обрабатывает через catch.
     */
    public Mission loadFromUrl(String url, Set<String> loadedKeys) throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url.trim()))
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200)
            throw new Exception("HTTP " + response.statusCode());

        String body = response.body();
        if (body == null || body.isBlank())
            throw new Exception("Сервер вернул пустой ответ");

        String format = detectFormatFromResponse(response);
        InputStream in = new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8));
        Mission mission = service.loadMission(in, format);

        String key = (mission.getMissionId() != null ? mission.getMissionId() : "") + ":" + format;
        if (!loadedKeys.add(key))
            throw new DuplicateMissionException(mission.getMissionId());

        return mission;
    }

    /** Исключение для дубликата — чтобы вызывающий код мог отличить его от сетевых ошибок */
    public static class DuplicateMissionException extends Exception {
        public DuplicateMissionException(String id) {
            super("Миссия " + id + " уже загружена.");
        }
    }

    // -------------------------------------------------------------------------
    // Загрузка из директории по URL
    // -------------------------------------------------------------------------

    /**
     * Загружает все миссии из HTTP-директории (листинг python -m http.server).
     * Показывает прогресс-бар. Результат возвращается через onComplete на EDT.
     */
    public void loadFromDirectory(JFrame parent, String baseUrl, Set<String> loadedKeys,
                                   Consumer<List<Mission>> onMissionsReady,
                                   Consumer<DirectoryLoadResult> onComplete) {
        String base = baseUrl.trim().endsWith("/") ? baseUrl.trim() : baseUrl.trim() + "/";

        // Прогресс-диалог
        JDialog progressDialog = new JDialog(parent, "Загрузка миссий", true);
        JProgressBar progressBar = new JProgressBar(0, 1);
        progressBar.setStringPainted(true);
        progressBar.setString("Подключение...");

        JLabel progressLabel = new JLabel("Подготовка...");
        progressLabel.setFont(UI_FONT);
        progressLabel.setBorder(new EmptyBorder(8, 12, 4, 12));

        JPanel progressPanel = new JPanel(new BorderLayout(0, 4));
        progressPanel.setBorder(new EmptyBorder(12, 12, 12, 12));
        progressPanel.add(progressLabel, BorderLayout.NORTH);
        progressPanel.add(progressBar, BorderLayout.CENTER);

        progressDialog.add(progressPanel);
        progressDialog.setSize(350, 120);
        progressDialog.setResizable(false);
        progressDialog.setLocationRelativeTo(parent);

        SwingWorker<DirectoryLoadResult, Object[]> worker = new SwingWorker<>() {

            @Override
            protected DirectoryLoadResult doInBackground() throws Exception {
                HttpClient client = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(10))
                        .build();

                // Получаем листинг директории
                HttpRequest dirRequest = HttpRequest.newBuilder()
                        .uri(URI.create(base))
                        .timeout(Duration.ofSeconds(15))
                        .GET()
                        .build();

                HttpResponse<String> dirResponse = client.send(dirRequest, HttpResponse.BodyHandlers.ofString());
                if (dirResponse.statusCode() != 200)
                    throw new Exception("HTTP " + dirResponse.statusCode());

                List<String> fileLinks = parseFileLinks(dirResponse.body());
                if (fileLinks.isEmpty())
                    throw new Exception("В папке не найдено файлов миссий (.json, .xml, .yaml, .txt).");

                publish(new Object[]{ "max", fileLinks.size() });

                List<String> errors     = new ArrayList<>();
                List<String> duplicates = new ArrayList<>();
                int loaded = 0;

                for (int i = 0; i < fileLinks.size(); i++) {
                    String fileName = fileLinks.get(i);
                    // показываем имя файла, который сейчас обрабатываем (до загрузки)
                    publish(new Object[]{ "label", fileName });

                    try {
                        String fileUrl = base + fileName.replace(" ", "%20");
                        HttpRequest req = HttpRequest.newBuilder()
                                .uri(URI.create(fileUrl))
                                .timeout(Duration.ofSeconds(15))
                                .GET()
                                .build();

                        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());

                        if (resp.statusCode() != 200) {
                            errors.add(fileName + ": HTTP " + resp.statusCode());
                            continue;
                        }

                        String format = detectFormatFromResponse(resp);
                        InputStream in = new ByteArrayInputStream(
                                resp.body().getBytes(StandardCharsets.UTF_8));
                        Mission mission = service.loadMission(in, format);

                        String key = fileName + ":" + format;
                        if (!loadedKeys.add(key)) {
                            duplicates.add(fileName);
                            continue;
                        }

                        // Отправляем каждую миссию сразу по мере загрузки
                        publish(new Object[]{ "mission", mission });
                        loaded++;
                    } catch (Exception e) {
                        errors.add(fileName + ": " + e.getMessage());
                    }

                    // прогресс инкрементируется ПОСЛЕ обработки файла —
                    // тогда счётчик «N/total» отражает реально завершённые попытки
                    // (загруженные + ошибки + дубликаты), а не просто отправленные.
                    publish(new Object[]{ "progress", i + 1, fileLinks.size() });
                }

                return new DirectoryLoadResult(loaded, fileLinks.size(), errors, duplicates);
            }

            @Override
            @SuppressWarnings("unchecked")
            protected void process(List<Object[]> chunks) {
                // Собираем все миссии из пачки в один список — UI перестраиваем
                // ОДИН раз на пачку, а не N раз. Без этого EDT захлёбывается:
                // rebuild() пересоздаёт все кнопки, что становится O(N²) на тысячах миссий.
                List<Mission> batchMissions = new ArrayList<>();
                Integer lastProgress = null, lastTotal = null;
                String lastLabel = null;

                for (Object[] msg : chunks) {
                    String type = (String) msg[0];
                    switch (type) {
                        case "max"      -> progressBar.setMaximum((Integer) msg[1]);
                        case "label"    -> lastLabel = (String) msg[1];
                        case "progress" -> { lastProgress = (Integer) msg[1]; lastTotal = (Integer) msg[2]; }
                        case "mission"  -> batchMissions.add((Mission) msg[1]);
                    }
                }

                // Применяем агрегированные обновления — каждое максимум 1 раз на пачку
                if (lastLabel != null) progressLabel.setText(lastLabel);
                if (lastProgress != null) {
                    progressBar.setValue(lastProgress);
                    progressBar.setString("Загрузка " + lastProgress + "/" + lastTotal + "...");
                }
                if (!batchMissions.isEmpty() && onMissionsReady != null) {
                    onMissionsReady.accept(batchMissions);
                }
            }

            @Override
            protected void done() {
                progressDialog.dispose();
                try {
                    onComplete.accept(get());
                } catch (Exception e) {
                    onComplete.accept(new DirectoryLoadResult(0, 0,
                            List.of(e.getMessage()), List.of()));
                }
            }
        };

        worker.execute();
        progressDialog.setVisible(true); // блокирует EDT до dispose()
    }

    // -------------------------------------------------------------------------
    // Вспомогательные методы
    // -------------------------------------------------------------------------

    /**
     * Определяет формат миссии из HTTP-ответа.
     * Приоритет: Content-Type → анализ содержимого через MissionService.
     */
    public String detectFormatFromResponse(HttpResponse<String> response) {
        String ct = response.headers().firstValue("Content-Type").orElse("");
        if (ct.contains("json"))                    return "json";
        if (ct.contains("xml"))                     return "xml";
        if (ct.contains("yaml") || ct.contains("yml")) return "yaml";
        return service.detectFormat(response.body());
    }

    /**
     * Извлекает имена файлов миссий из HTML-листинга директории.
     * Поддерживает .json, .xml, .yaml, .yml, .txt и файлы без расширения (pipe).
     */
    public List<String> parseFileLinks(String html) {
        Set<String> supportedExt = Set.of("json", "xml", "yaml", "yml", "txt");
        List<String> result = new ArrayList<>();
        Pattern pattern = Pattern.compile("href=\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(html);

        while (matcher.find()) {
            String href = matcher.group(1);
            if (href.startsWith("/") || href.startsWith("?") || href.startsWith("..")) continue;
            if (href.endsWith("/")) continue;

            String decoded = java.net.URLDecoder.decode(href, StandardCharsets.UTF_8);
            int dot = decoded.lastIndexOf('.');
            if (dot >= 0) {
                String ext = decoded.substring(dot + 1).toLowerCase();
                if (supportedExt.contains(ext)) { result.add(decoded); continue; }
            }
            if (dot < 0 && !decoded.contains(".")) result.add(decoded);
        }
        return result;
    }
}
