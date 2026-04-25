package ui;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import parser.plugin.PluginType;
import service.AppLogger;
import service.PluginManager;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Диалог маркетплейса плагинов — каталог доступных плагинов с возможностью скачать.
 * Каталог загружается из /marketplace.json в ресурсах приложения.
 */
public class MarketplaceDialog extends JDialog {

    private static final Font UI_FONT = new Font("Segoe UI", Font.PLAIN, 14);
    private static final Font UI_BOLD = new Font("Segoe UI", Font.BOLD, 14);
    private static final Font TITLE_FONT = new Font("Segoe UI", Font.BOLD, 15);
    private static final Font DESC_FONT = new Font("Segoe UI", Font.PLAIN, 13);

    /** Внутренняя модель одного плагина из каталога */
    private record MarketplaceEntry(
            String id, String name, PluginType type,
            String version, String author, String description,
            String downloadUrl, String jarFileName
    ) {}

    /** Callback — вызывается после успешной установки плагина (reload + обновление UI) */
    private final Runnable onPluginInstalled;

    public MarketplaceDialog(Dialog parent, Runnable onPluginInstalled) {
        super(parent, "Маркет плагинов", true);
        this.onPluginInstalled = onPluginInstalled;
        init(null);
    }

    public MarketplaceDialog(JFrame parent) {
        super(parent, "Маркет плагинов", true);
        this.onPluginInstalled = null;
        init(parent);
    }

    /** Инициализация окна */
    private void init(Component owner) {
        setSize(560, 620);
        setLocationRelativeTo(owner);
        setResizable(false);

        List<MarketplaceEntry> entries = loadEntries();
        buildUI(entries);
    }

    /** Загружает список плагинов из marketplace.json в ресурсах */
    private List<MarketplaceEntry> loadEntries() {
        List<MarketplaceEntry> list = new ArrayList<>();
        try (InputStream in = getClass().getResourceAsStream("/marketplace.json")) {
            if (in == null) return list;
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(in);
            JsonNode plugins = root.get("plugins");
            if (plugins == null || !plugins.isArray()) return list;

            for (JsonNode node : plugins) {
                // Парсим тип плагина
                PluginType type = PluginType.PARSER;
                try { type = PluginType.valueOf(node.path("type").asText("PARSER")); }
                catch (IllegalArgumentException ignored) {}

                list.add(new MarketplaceEntry(
                        node.path("id").asText(),
                        node.path("name").asText(),
                        type,
                        node.path("version").asText("1.0"),
                        node.path("author").asText("—"),
                        node.path("description").asText(""),
                        node.path("downloadUrl").asText(""),
                        node.path("jarFileName").asText("")
                ));
            }
        } catch (Exception e) {
            AppLogger.error("Маркет: ошибка загрузки marketplace.json — " + e.getMessage());
        }
        return list;
    }

    /** Построение UI с вкладками по типам плагинов */
    private void buildUI(List<MarketplaceEntry> entries) {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBackground(AppTheme.bgMain());
        root.setBorder(new EmptyBorder(10, 12, 10, 12));

        // Заголовок
        JLabel title = new JLabel("Маркет плагинов");
        title.setFont(TITLE_FONT);
        title.setForeground(AppTheme.text());
        title.setBorder(new EmptyBorder(0, 0, 6, 0));

        // Вкладки по типам
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(UI_BOLD);
        tabs.setBackground(AppTheme.bgMain());

        // Наполняем вкладки
        tabs.addTab("Парсеры", buildTabPanel(
                entries.stream().filter(e -> e.type() == PluginType.PARSER).toList()
        ));
        tabs.addTab("Экспорт", buildTabPanel(
                entries.stream().filter(e -> e.type() == PluginType.EXPORT).toList()
        ));
        tabs.addTab("Отчёт", buildTabPanel(
                entries.stream().filter(e -> e.type() == PluginType.REPORT_FORMATTER).toList()
        ));

        // Кнопка закрыть
        JButton closeBtn = createSecondaryButton("Закрыть");
        closeBtn.addActionListener(e -> dispose());

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        bottom.setBackground(AppTheme.bgMain());
        bottom.setBorder(new EmptyBorder(6, 0, 0, 0));
        bottom.add(closeBtn);

        root.add(title, BorderLayout.NORTH);
        root.add(tabs, BorderLayout.CENTER);
        root.add(bottom, BorderLayout.SOUTH);

        setContentPane(root);
    }

    /** Строит панель-вкладку с карточками плагинов */
    private JPanel buildTabPanel(List<MarketplaceEntry> entries) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(AppTheme.bgMain());
        panel.setBorder(new EmptyBorder(8, 4, 8, 4));

        if (entries.isEmpty()) {
            // Сообщение об отсутствии плагинов в категории
            JLabel empty = new JLabel("Нет доступных плагинов");
            empty.setFont(UI_FONT);
            empty.setForeground(AppTheme.textSecondary());
            empty.setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(empty);
        } else {
            for (MarketplaceEntry entry : entries) {
                panel.add(buildCard(entry));
                panel.add(Box.createVerticalStrut(8));
            }
        }

        JScrollPane scroll = new JScrollPane(panel);
        scroll.getViewport().setBackground(AppTheme.bgMain());
        scroll.setBackground(AppTheme.bgMain());
        scroll.setBorder(null);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        // Обёртка-панель для scroll — JTabbedPane ожидает Component
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(AppTheme.bgMain());
        wrapper.add(scroll, BorderLayout.CENTER);
        return wrapper;
    }

    /** Карточка одного плагина: имя, автор, версия, описание, кнопка Скачать */
    private JPanel buildCard(MarketplaceEntry entry) {
        JPanel card = new JPanel(new BorderLayout(8, 4));
        card.setBackground(AppTheme.bgPanel());
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
        card.setBorder(new CompoundBorder(
                new LineBorder(AppTheme.border(), 1, true),
                new EmptyBorder(10, 12, 10, 12)
        ));

        // Заголовок карточки: имя + автор + версия
        JLabel nameLabel = new JLabel(entry.name());
        nameLabel.setFont(UI_BOLD);
        nameLabel.setForeground(AppTheme.text());

        JLabel metaLabel = new JLabel(entry.author() + " · v" + entry.version());
        metaLabel.setFont(DESC_FONT);
        metaLabel.setForeground(AppTheme.textSecondary());

        JPanel header = new JPanel(new BorderLayout(4, 2));
        header.setOpaque(false);
        header.add(nameLabel, BorderLayout.NORTH);
        header.add(metaLabel, BorderLayout.SOUTH);

        // Описание плагина
        JLabel descLabel = new JLabel("<html><body style='width:300px'>" + entry.description() + "</body></html>");
        descLabel.setFont(DESC_FONT);
        descLabel.setForeground(AppTheme.text());

        // Кнопка скачать
        JButton downloadBtn = createPrimaryButton("Скачать");
        downloadBtn.addActionListener(e -> downloadPlugin(entry, downloadBtn));

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        right.setOpaque(false);
        right.add(downloadBtn);

        card.add(header, BorderLayout.NORTH);
        card.add(descLabel, BorderLayout.CENTER);
        card.add(right, BorderLayout.EAST);

        return card;
    }

    /**
     * Скачивает JAR-файл плагина в папку parser-plugins/.
     * Использует SwingWorker — не блокирует EDT.
     */
    private void downloadPlugin(MarketplaceEntry entry, JButton btn) {
        btn.setEnabled(false);
        btn.setText("Загрузка...");

        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                // Ищем папку parser-plugins/ рядом с рабочей директорией
                Path pluginsDir = findOrCreatePluginsDir();

                // NORMAL — следуем редиректам (GitHub Releases отвечает 302 → CDN)
                HttpClient client = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(15))
                        .followRedirects(HttpClient.Redirect.NORMAL)
                        .build();

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(entry.downloadUrl()))
                        .timeout(Duration.ofSeconds(60))
                        .GET()
                        .build();

                HttpResponse<byte[]> response = client.send(request,
                        HttpResponse.BodyHandlers.ofByteArray());

                if (response.statusCode() != 200) {
                    throw new Exception("HTTP " + response.statusCode());
                }

                // Сохраняем JAR в папку плагинов
                Path jarPath = pluginsDir.resolve(entry.jarFileName());
                Files.write(jarPath, response.body());
                AppLogger.log("Плагин скачан: " + jarPath);
                return true;
            }

            @Override
            protected void done() {
                try {
                    get(); // проверяем исключения из doInBackground
                    // Вызываем callback — он делает полный reload (PluginManager + ParserRegistry)
                    if (onPluginInstalled != null) {
                        onPluginInstalled.run();
                    } else {
                        PluginManager.getInstance().reload();
                    }
                    JOptionPane.showMessageDialog(MarketplaceDialog.this,
                            "Плагин «" + entry.name() + "» установлен!\n"
                                    + "Плагин активирован и готов к использованию.",
                            "Установлено", JOptionPane.INFORMATION_MESSAGE);
                    btn.setText("Установлено ✓");
                } catch (Exception e) {
                    AppLogger.error("Маркет: ошибка скачивания " + entry.name() + " — " + e.getMessage());
                    JOptionPane.showMessageDialog(MarketplaceDialog.this,
                            "Ошибка скачивания:\n" + e.getMessage(),
                            "Ошибка", JOptionPane.ERROR_MESSAGE);
                    btn.setText("Скачать");
                    btn.setEnabled(true);
                }
            }
        };

        worker.execute();
    }

    /**
     * Возвращает папку parser-plugins/ — ту же, которую использует PluginManager.
     * Приоритет: Maven-корень (где pom.xml) → рядом с классами → рабочая директория.
     * Если папка не существует — создаёт её.
     */
    private Path findOrCreatePluginsDir() throws Exception {
        // Пробуем найти корень Maven-модуля (там уже есть parser-plugins/)
        try {
            Path classLoc = Path.of(getClass().getProtectionDomain()
                    .getCodeSource().getLocation().toURI());
            Path base = Files.isDirectory(classLoc) ? classLoc : classLoc.getParent();
            if (base != null) {
                Path cur = base;
                while (cur != null) {
                    Path candidate = cur.resolve("parser-plugins");
                    if (Files.isDirectory(candidate)) return candidate;
                    if (Files.exists(cur.resolve("pom.xml"))) {
                        // нашли корень — создаём папку здесь
                        Files.createDirectories(candidate);
                        return candidate;
                    }
                    cur = cur.getParent();
                }
            }
        } catch (Exception ignored) {}

        // Fallback: рабочая директория
        Path dir = Path.of("parser-plugins");
        Files.createDirectories(dir);
        return dir;
    }

    // --- Фабричные методы кнопок ---

    private JButton createPrimaryButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(UI_BOLD);
        btn.setForeground(AppTheme.text());
        btn.setBackground(AppTheme.bgPanel());
        btn.setFocusPainted(false);
        btn.setBorder(new CompoundBorder(
                new LineBorder(AppTheme.border(), 1, true),
                new EmptyBorder(6, 14, 6, 14)
        ));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JButton createSecondaryButton(String text) {
        JButton btn = createPrimaryButton(text);
        btn.setBackground(AppTheme.bgSecondary());
        return btn;
    }
}
