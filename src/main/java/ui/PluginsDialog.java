package ui;

import parser.MissionParser;
import parser.plugin.ArchivePlugin;
import parser.plugin.ExportPlugin;
import parser.plugin.PluginMetadata;
import parser.plugin.ReportFormatterPlugin;
import service.PluginManager;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.List;

/**
 * Диалог управления плагинами — отображает список установленных плагинов
 * и позволяет включать/отключать каждый из них.
 */
public class PluginsDialog extends JDialog {

    private static final Font UI_FONT = new Font("Segoe UI", Font.PLAIN, 14);
    private static final Font UI_BOLD = new Font("Segoe UI", Font.BOLD, 14);
    private static final Font TITLE_FONT = new Font("Segoe UI", Font.BOLD, 13);

    private final Runnable onReload;
    private JLabel statusLabel;
    private JPanel parsersContent;
    private JPanel exportsContent;
    private JPanel reportsContent;

    public PluginsDialog(JFrame parent, Runnable onReload) {
        super(parent, "Плагины", true);
        this.onReload = onReload;

        setSize(500, 480);
        setLocationRelativeTo(parent);
        setResizable(false);

        buildUI();
    }

    /** Построение интерфейса диалога */
    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBackground(AppTheme.bgMain());
        root.setBorder(new EmptyBorder(10, 12, 10, 12));

        // Верхняя панель с кнопками
        root.add(buildTopPanel(), BorderLayout.NORTH);

        // Центральная область со списками плагинов
        JScrollPane scroll = buildCenterScrollPane();
        root.add(scroll, BorderLayout.CENTER);

        // Нижняя панель со статусом и кнопкой Закрыть
        root.add(buildBottomPanel(), BorderLayout.SOUTH);

        setContentPane(root);
    }

    /** Верхняя панель: кнопка Обновить слева, кнопка Маркет справа */
    private JPanel buildTopPanel() {
        JButton refreshBtn = createPrimaryButton("Обновить");
        JButton marketBtn = createSecondaryButton("Маркет плагинов");

        // Перезагружаем плагины и перестраиваем список.
        // onReload.run() уже делает PluginManager.reload() внутри — двойной вызов не нужен.
        refreshBtn.addActionListener(e -> {
            onReload.run();
            refreshContent();
        });

        // Открываем маркетплейс — передаём тот же callback, чтобы после установки
        // плагин сразу появился в списке без ручного нажатия «Обновить».
        marketBtn.addActionListener(e ->
                new MarketplaceDialog(this, () -> { onReload.run(); refreshContent(); })
                        .setVisible(true)
        );

        JPanel panel = new JPanel(new BorderLayout(6, 0));
        panel.setBackground(AppTheme.bgMain());
        panel.setBorder(new EmptyBorder(0, 0, 6, 0));
        panel.add(refreshBtn, BorderLayout.WEST);
        panel.add(marketBtn, BorderLayout.EAST);
        return panel;
    }

    /** Центральная область: три секции — Парсеры, Экспорт, Форматтеры отчётов */
    private JScrollPane buildCenterScrollPane() {
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setBackground(AppTheme.bgMain());

        // Секция парсеров
        parsersContent = new JPanel();
        parsersContent.setLayout(new BoxLayout(parsersContent, BoxLayout.Y_AXIS));
        parsersContent.setBackground(AppTheme.bgPanel());
        centerPanel.add(buildSection("Парсеры форматов", parsersContent));
        centerPanel.add(Box.createVerticalStrut(8));

        // Секция экспорта
        exportsContent = new JPanel();
        exportsContent.setLayout(new BoxLayout(exportsContent, BoxLayout.Y_AXIS));
        exportsContent.setBackground(AppTheme.bgPanel());
        centerPanel.add(buildSection("Плагины экспорта", exportsContent));
        centerPanel.add(Box.createVerticalStrut(8));

        // Секция форматтеров отчётов
        reportsContent = new JPanel();
        reportsContent.setLayout(new BoxLayout(reportsContent, BoxLayout.Y_AXIS));
        reportsContent.setBackground(AppTheme.bgPanel());
        centerPanel.add(buildSection("Форматтеры отчётов", reportsContent));

        // Заполняем содержимое данными
        fillParsersContent();
        fillExportsContent();
        fillReportsContent();

        JScrollPane scroll = new JScrollPane(centerPanel);
        scroll.getViewport().setBackground(AppTheme.bgMain());
        scroll.setBackground(AppTheme.bgMain());
        scroll.setBorder(null);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        return scroll;
    }

    /** Создаёт секцию с заголовком и переданным контентом */
    private JPanel buildSection(String title, JPanel contentPanel) {
        JPanel section = new JPanel(new BorderLayout());
        section.setBackground(AppTheme.bgPanel());
        section.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        TitledBorder border = BorderFactory.createTitledBorder(
                new CompoundBorder(
                        new LineBorder(AppTheme.border(), 1, true),
                        new EmptyBorder(6, 6, 6, 6)
                ),
                title, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION,
                TITLE_FONT, AppTheme.text()
        );
        section.setBorder(border);
        section.add(contentPanel, BorderLayout.CENTER);
        return section;
    }

    /** Заполняет секцию парсеров: плагины форматов + архивные распаковщики */
    private void fillParsersContent() {
        parsersContent.removeAll();
        PluginManager pm = PluginManager.getInstance();
        pm.loadIfNeeded();

        List<MissionParser>  parsers  = pm.getAllParserPlugins();
        List<ArchivePlugin>  archives = pm.getAllArchivePlugins();

        if (parsers.isEmpty() && archives.isEmpty()) {
            parsersContent.add(makeEmptyLabel("Плагины-парсеры не установлены"));
        } else {
            for (MissionParser parser : parsers) {
                PluginMetadata meta = parser.getMetadata();
                parsersContent.add(buildPluginRow(meta, false, pm.isEnabled(meta.id())));
                parsersContent.add(Box.createVerticalStrut(4));
            }
            // Архивные плагины отображаются в той же секции — для пользователя это парсеры
            for (ArchivePlugin ap : archives) {
                PluginMetadata meta = ap.getMetadata();
                parsersContent.add(buildPluginRow(meta, false, pm.isEnabled(meta.id())));
                parsersContent.add(Box.createVerticalStrut(4));
            }
        }
        parsersContent.revalidate();
        parsersContent.repaint();
    }

    /** Заполняет секцию экспортных плагинов */
    private void fillExportsContent() {
        exportsContent.removeAll();
        PluginManager pm = PluginManager.getInstance();
        pm.loadIfNeeded();

        List<ExportPlugin> plugins = pm.getAllExportPlugins();
        if (plugins.isEmpty()) {
            // Сообщение об отсутствии плагинов экспорта
            JLabel empty = makeEmptyLabel("Плагины экспорта не установлены");
            exportsContent.add(empty);
        } else {
            for (ExportPlugin ep : plugins) {
                PluginMetadata meta = ep.getMetadata();
                boolean enabled = pm.isEnabled(meta.id());
                exportsContent.add(buildPluginRow(meta, false, enabled));
                exportsContent.add(Box.createVerticalStrut(4));
            }
        }
        exportsContent.revalidate();
        exportsContent.repaint();
    }

    /** Строка одного плагина: чекбокс + имя + версия + автор */
    private JPanel buildPluginRow(PluginMetadata meta, boolean builtin, boolean enabled) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setBackground(AppTheme.bgPanel());
        row.setBorder(new CompoundBorder(
                new LineBorder(AppTheme.border(), 1, true),
                new EmptyBorder(6, 8, 6, 8)
        ));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

        // Чекбокс включения/отключения (для встроенных — задизейблен)
        JCheckBox checkBox = new JCheckBox();
        checkBox.setSelected(enabled);
        checkBox.setBackground(AppTheme.bgPanel());
        checkBox.setEnabled(!builtin);
        checkBox.addActionListener(e -> {
            PluginManager.getInstance().setEnabled(meta.id(), checkBox.isSelected());
            onReload.run();
        });

        // Имя плагина
        JLabel nameLabel = new JLabel(meta.name());
        nameLabel.setFont(UI_BOLD);
        nameLabel.setForeground(builtin ? AppTheme.textSecondary() : AppTheme.text());

        // Версия и автор — правее
        JLabel infoLabel = new JLabel("v" + meta.version() + "  |  " + meta.author());
        infoLabel.setFont(UI_FONT);
        infoLabel.setForeground(AppTheme.textSecondary());

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        left.setOpaque(false);
        left.add(checkBox);
        left.add(nameLabel);

        row.add(left, BorderLayout.WEST);
        row.add(infoLabel, BorderLayout.EAST);
        return row;
    }

    /** Метка для пустого состояния секции */
    private JLabel makeEmptyLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(UI_FONT);
        label.setForeground(AppTheme.textSecondary());
        label.setBorder(new EmptyBorder(8, 8, 8, 8));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    /** Заполняет секцию форматтеров отчётов */
    private void fillReportsContent() {
        reportsContent.removeAll();
        PluginManager pm = PluginManager.getInstance();
        pm.loadIfNeeded();

        var plugins = pm.getAllReportPlugins();
        if (plugins.isEmpty()) {
            reportsContent.add(makeEmptyLabel("Плагины форматтеров не установлены"));
        } else {
            for (var plugin : plugins) {
                var meta = plugin.getMetadata();
                boolean enabled = pm.isEnabled(meta.id());
                reportsContent.add(buildPluginRow(meta, false, enabled));
                reportsContent.add(Box.createVerticalStrut(4));
            }
        }
        reportsContent.revalidate();
        reportsContent.repaint();
    }

    /** Обновляет содержимое без закрытия диалога */
    private void refreshContent() {
        fillParsersContent();
        fillExportsContent();
        fillReportsContent();
        updateStatus();
    }

    /** Нижняя панель: статус слева, кнопка Закрыть справа */
    private JPanel buildBottomPanel() {
        statusLabel = new JLabel();
        statusLabel.setFont(UI_FONT);
        statusLabel.setForeground(AppTheme.textSecondary());
        updateStatus();

        JButton closeBtn = createSecondaryButton("Закрыть");
        closeBtn.addActionListener(e -> dispose());

        JPanel panel = new JPanel(new BorderLayout(6, 0));
        panel.setBackground(AppTheme.bgMain());
        panel.setBorder(new EmptyBorder(6, 0, 0, 0));
        panel.add(statusLabel, BorderLayout.WEST);
        panel.add(closeBtn, BorderLayout.EAST);
        return panel;
    }

    /** Обновляет статусную строку с количеством загруженных плагинов */
    private void updateStatus() {
        PluginManager.getInstance().loadIfNeeded();
        PluginManager.LoadResult result = PluginManager.getInstance().getLastResult();
        if (result == null) {
            statusLabel.setText("Загрузка...");
        } else if (!result.folderExists()) {
            statusLabel.setText("Папка parser-plugins/ не найдена");
        } else {
            int total = result.parsersFound() + result.exportsFound() + result.reportsFound();
            statusLabel.setText("Загружено: " + total + " плагин(ов)");
        }
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
                new EmptyBorder(8, 16, 8, 16)
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
