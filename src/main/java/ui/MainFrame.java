package ui;

import ai.AiReviewService;
import ai.AiServiceFactory;
import model.Mission;
import service.*;
import service.filter.FilterChain;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.io.File;
import java.net.http.HttpConnectTimeoutException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Главное окно приложения.
 * Отвечает только за компоновку UI и оркестрацию: делегирует загрузку в MissionLoader,
 * а управление списком миссий — в MissionListPanel.
 */
public class MainFrame extends JFrame implements MissionListPanel.Host {

    // --- данные ---
    private final List<Mission>   loadedMissions = new ArrayList<>();
    private final List<File>      loadedFiles    = new ArrayList<>();
    private final Set<String>     loadedKeys     = new HashSet<>();
    private final Set<Mission>    favorites      = new HashSet<>();
    private final FilterChain     filterChain    = new FilterChain();
    private Mission               currentMission;

    // --- сервисы ---
    private final MissionService       missionService  = new MissionService();
    private final MissionBatchService  batchService    = new MissionBatchService();
    private final BatchStatsFormatter  statsFormatter  = new BatchStatsFormatter();
    private final AiReviewService      aiReviewService = AiServiceFactory.create();
    private final MissionLoader        missionLoader   = new MissionLoader(missionService);
    private final ExportController     exportController = new ExportController(this);

    // --- отчёты ---
    private final JComboBox<ReportFormatter>   reportTypeSelector;
    private final StyledReportPane             reportArea = new StyledReportPane();

    // --- UI ---
    private MissionListPanel missionListPanel;

    // --- иконки помощника ---
    private ImageIcon assistantIcon;
    private ImageIcon assistantSleepIcon;

    // --- шрифты ---
    private static final Font UI_FONT    = new Font("Segoe UI", Font.PLAIN, 14);
    private static final Font UI_BOLD    = new Font("Segoe UI", Font.BOLD,  14);
    private static final Font TITLE_FONT = new Font("Segoe UI", Font.BOLD,  15);

    public MainFrame() {
        reportTypeSelector = new JComboBox<>();
        fillReportSelector(); // заполняем с учётом текущего состояния плагинов
        reportTypeSelector.setRenderer((list, value, index, isSelected, cellHasFocus) -> {
            JLabel label = new JLabel(value != null ? value.getDisplayName() : "");
            label.setFont(UI_BOLD);
            label.setBorder(new EmptyBorder(6, 10, 6, 10));
            label.setOpaque(true);
            label.setBackground(isSelected ? list.getSelectionBackground() : AppTheme.bgPanel());
            label.setForeground(AppTheme.text());
            return label;
        });
        reportTypeSelector.addActionListener(e -> refreshReport());

        loadAssistantIcons();
        setupWindow();
        applySettings();

        // Показываем уведомление о загруженных плагинах — после отрисовки окна
        SwingUtilities.invokeLater(() -> PluginStartupToast.showIfNeeded(this));
    }

    // =========================================================================
    // MissionListPanel.Host — реализация контракта
    // =========================================================================

    @Override public JFrame         getFrame()             { return this; }
    @Override public List<Mission>  getMissions()          { return loadedMissions; }
    @Override public Set<Mission>   getFavorites()         { return favorites; }
    @Override public FilterChain    getFilterChain()       { return filterChain; }
    @Override public Mission        getCurrentMission()    { return currentMission; }
    @Override public String         getReportText()        { return reportArea.getText(); }
    @Override public AiReviewService getAiService()        { return aiReviewService; }
    @Override public ImageIcon      getAssistantSleepIcon(){ return assistantSleepIcon; }

    @Override
    public void onMissionSelected(int index) {
        currentMission = loadedMissions.get(index);
        refreshReport();
    }

    @Override
    public void onShowStats() {
        showStats();
    }

    @Override
    public void onClearConfirmed() {
        loadedFiles.clear();
        loadedMissions.clear();
        loadedKeys.clear();
        favorites.clear();
        currentMission = null;
        reportArea.setStyledText("");
        removeChartsButton();
        missionListPanel.rebuild();
    }

    // =========================================================================
    // Инициализация окна
    // =========================================================================

    private void loadAssistantIcons() {
        assistantIcon      = loadScaledIcon("/templates/assets/Assistant-stand.png", 88);
        assistantSleepIcon = loadScaledIcon("/templates/assets/Assistant-sleep.png",  88);
    }

    private ImageIcon loadScaledIcon(String resourcePath, int size) {
        try {
            java.net.URL url = getClass().getResource(resourcePath);
            if (url == null) return null;
            ImageIcon original = new ImageIcon(url);
            Image scaled = original.getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH);
            return new ImageIcon(scaled);
        } catch (Exception ignored) { return null; }
    }

    private void setupWindow() {
        setTitle("Mission Analyzer");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(8, 8));
        getContentPane().setBackground(AppTheme.bgMain());

        add(buildTopPanel(),    BorderLayout.NORTH);
        add(buildCenterPanel(), BorderLayout.CENTER);
    }

    // =========================================================================
    // Компоновка панелей
    // =========================================================================

    private JPanel buildTopPanel() {
        JButton chooseBtn = createPrimaryButton("Загрузить");
        JButton exportBtn = createPrimaryButton("Экспорт ▼");

        ImageIcon settingsIcon = null;
        try {
            java.net.URL url = getClass().getResource("/templates/assets/settings.png");
            if (url != null) {
                ImageIcon orig = new ImageIcon(url);
                Image scaled = orig.getImage().getScaledInstance(18, 18, Image.SCALE_SMOOTH);
                settingsIcon = new ImageIcon(scaled);
            }
        } catch (Exception ignored) {}

        JButton settingsBtn = createSettingsButton(settingsIcon);
        JButton logsBtn     = createSecondaryButton("Логи");

        styleComboBox(reportTypeSelector);

        chooseBtn.addActionListener(e -> showLoadDialog());
        exportBtn.addActionListener(e -> exportController.showExportMenu(
                exportBtn, reportArea.getText(),
                (ReportFormatter) reportTypeSelector.getSelectedItem()
        ));
        settingsBtn.addActionListener(e -> openSettings());
        logsBtn.addActionListener(e -> showLogs());

        JPanel panel = new JPanel(new BorderLayout(12, 0));
        panel.setBackground(AppTheme.bgTop());
        panel.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 1, 0, AppTheme.border()),
                new EmptyBorder(10, 12, 10, 12)
        ));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);
        JLabel reportTypeLabel = new JLabel("Тип отчёта:");
        reportTypeLabel.setFont(UI_BOLD);
        reportTypeLabel.setForeground(AppTheme.text());
        left.add(chooseBtn);
        left.add(reportTypeLabel);
        left.add(reportTypeSelector);
        left.add(exportBtn);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        right.setOpaque(false);
        right.add(settingsBtn);
        right.add(logsBtn);

        panel.add(left,  BorderLayout.WEST);
        panel.add(right, BorderLayout.EAST);
        return panel;
    }

    private JSplitPane buildCenterPanel() {
        // Левая панель — делегирована в MissionListPanel
        missionListPanel = new MissionListPanel(this);

        // Правая панель — область отчёта
        reportArea.setReportFontFamily("Consolas");
        reportArea.setReportFontSize(SettingsDialog.getFontSize());
        reportArea.setForeground(AppTheme.text());
        reportArea.setBackground(AppTheme.bgPanel());
        reportArea.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));

        JScrollPane reportScroll = new JScrollPane(reportArea);
        styleScrollPane(reportScroll);
        reportScroll.setBorder(createTitledBorder("Отчёт"));

        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBackground(AppTheme.bgMain());
        rightPanel.setBorder(new EmptyBorder(8, 0, 8, 8));
        rightPanel.add(reportScroll, BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, missionListPanel, rightPanel);
        split.setDividerLocation(280);
        split.setResizeWeight(0.0);
        split.setBorder(null);
        split.setBackground(AppTheme.bgMain());
        split.setDividerSize(8);
        return split;
    }

    // =========================================================================
    // Загрузка миссий — диалог выбора источника
    // =========================================================================

    private void showLoadDialog() {
        JDialog dialog = new JDialog(this, "Источник данных", true);
        dialog.setLayout(new BorderLayout());
        dialog.setResizable(false);

        JLabel title = new JLabel("Откуда загрузить миссии?");
        title.setFont(TITLE_FONT);
        title.setForeground(AppTheme.text());
        title.setHorizontalAlignment(SwingConstants.CENTER);
        title.setBorder(new EmptyBorder(16, 16, 8, 16));

        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.Y_AXIS));
        buttonsPanel.setBackground(AppTheme.bgMain());
        buttonsPanel.setBorder(new EmptyBorder(4, 16, 16, 16));

        JButton fromFilesBtn = createLoadOptionButton("Из файлов");
        JButton fromUrlBtn   = createLoadOptionButton("Из URL (одна миссия)");
        JButton fromDirBtn   = createLoadOptionButton("Из URL (папка — все миссии)");

        fromFilesBtn.addActionListener(e -> { dialog.dispose(); chooseFiles(); });
        fromUrlBtn.addActionListener(e ->   { dialog.dispose(); loadFromUrl(); });
        fromDirBtn.addActionListener(e ->   { dialog.dispose(); loadFromDirectoryUrl(); });

        buttonsPanel.add(fromFilesBtn);
        buttonsPanel.add(Box.createVerticalStrut(8));
        buttonsPanel.add(fromUrlBtn);
        buttonsPanel.add(Box.createVerticalStrut(8));
        buttonsPanel.add(fromDirBtn);

        dialog.getContentPane().setBackground(AppTheme.bgMain());
        dialog.add(title,        BorderLayout.NORTH);
        dialog.add(buttonsPanel, BorderLayout.CENTER);
        dialog.pack();
        dialog.setSize(new Dimension(340, dialog.getHeight()));
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private JButton createLoadOptionButton(String text) {
        JButton btn = new JButton(text);
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        btn.setFont(UI_BOLD);
        btn.setForeground(AppTheme.text());
        btn.setBackground(AppTheme.bgSecondary());
        btn.setFocusPainted(false);
        btn.setBorder(new CompoundBorder(
                new LineBorder(AppTheme.border(), 1, true),
                new EmptyBorder(8, 16, 8, 16)
        ));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    // -------------------------------------------------------------------------

    private void chooseFiles() {
        JFileChooser fc = new JFileChooser();
        fc.setMultiSelectionEnabled(true);
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        MissionLoader.FileLoadResult result =
                missionLoader.loadFiles(fc.getSelectedFiles(), loadedKeys);

        if (!result.duplicates().isEmpty())
            warn("Уже загружены (пропущены): " + String.join(", ", result.duplicates()));
        if (!result.errors().isEmpty())
            warn("Не удалось загрузить:\n" + String.join("\n", result.errors()));

        loadedMissions.addAll(result.loaded());
        loadedFiles.addAll(result.files());

        missionListPanel.rebuild();

        if (!result.loaded().isEmpty()) {
            Mission first = result.loaded().getFirst();
            currentMission = first;
            refreshReport();
            missionListPanel.setSelectedMission(first);
        }
    }

    private void loadFromUrl() {
        String url = JOptionPane.showInputDialog(
                this, "Введите URL для загрузки миссий (REST API):",
                "Загрузка из URL", JOptionPane.PLAIN_MESSAGE);
        if (url == null || url.isBlank()) return;

        try {
            Mission mission = missionLoader.loadFromUrl(url, loadedKeys);
            loadedMissions.add(mission);
            missionListPanel.rebuild();
            currentMission = mission;
            refreshReport();
            missionListPanel.setSelectedMission(mission);
        } catch (MissionLoader.DuplicateMissionException e) {
            warn(e.getMessage());
        } catch (HttpConnectTimeoutException e) {
            AppLogger.error("Таймаут подключения к " + url);
            warn("Не удалось подключиться к серверу (таймаут).");
        } catch (IllegalArgumentException e) {
            AppLogger.error("Некорректный URL: " + url + " — " + e.getMessage());
            warn("Некорректный URL:\n" + e.getMessage());
        } catch (Exception e) {
            AppLogger.error("Ошибка загрузки из URL: " + url + " — " + e.getMessage());
            warn("Не удалось загрузить миссию из URL:\n" + e.getMessage());
        }
    }

    private void loadFromDirectoryUrl() {
        String baseUrl = JOptionPane.showInputDialog(
                this, "Введите URL папки с миссиями\n(например, http://localhost:8000/):",
                "Загрузка из папки", JOptionPane.PLAIN_MESSAGE);
        if (baseUrl == null || baseUrl.isBlank()) return;

        missionLoader.loadFromDirectory(
                this, baseUrl, loadedKeys,
                // onMissionsReady — добавляем каждую миссию сразу по мере загрузки
                missions -> {
                    loadedMissions.addAll(missions);
                    missionListPanel.rebuild();
                },
                // onComplete — итоговое сообщение
                result -> {
                    if (result.loaded() == 0 && result.errors().size() == 1
                            && result.duplicates().isEmpty()) {
                        // единственная ошибка — проблема с директорией
                        warn("Ошибка загрузки:\n" + result.errors().getFirst());
                        return;
                    }

                    // Выделяем первую загруженную миссию если ничего не было выбрано
                    if (!loadedMissions.isEmpty() && currentMission == null) {
                        currentMission = loadedMissions.getFirst();
                        refreshReport();
                        missionListPanel.setSelectedMission(currentMission);
                    }

                    StringBuilder msg = new StringBuilder();
                    msg.append("Загружено: ").append(result.loaded())
                       .append(" из ").append(result.total());
                    if (!result.duplicates().isEmpty())
                        msg.append("\n\nПропущены (дубликаты): ")
                           .append(String.join(", ", result.duplicates()));
                    if (!result.errors().isEmpty())
                        msg.append("\n\nОшибки:\n").append(String.join("\n", result.errors()));

                    int msgType = result.errors().isEmpty()
                            ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE;
                    JOptionPane.showMessageDialog(this, msg.toString(), "Результат загрузки", msgType);
                }
        );
    }

    // =========================================================================
    // Отчёты и статистика
    // =========================================================================

    private void refreshReport() {
        if (currentMission == null) return;
        ReportFormatter formatter = (ReportFormatter) reportTypeSelector.getSelectedItem();
        if (formatter == null) return;
        reportArea.setStyledText(formatter.format(currentMission));
        removeChartsButton();
    }

    private void showStats() {
        currentMission = null;
        reportArea.setStyledText(statsFormatter.format(loadedMissions));
        reportArea.setCaretPosition(0);
        showChartsButton();
    }

    private void showChartsButton() {
        JScrollPane reportScroll = (JScrollPane) reportArea.getParent().getParent();
        Container rightPanel = reportScroll.getParent();
        removeChartsButton();

        JButton chartsBtn = new JButton("Показать графики");
        chartsBtn.setFont(UI_BOLD);
        chartsBtn.setForeground(AppTheme.text());
        chartsBtn.setBackground(AppTheme.bgSecondary());
        chartsBtn.setFocusPainted(false);
        chartsBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        chartsBtn.setBorder(new CompoundBorder(
                new LineBorder(AppTheme.border(), 1, true),
                new EmptyBorder(8, 16, 8, 16)
        ));
        chartsBtn.addActionListener(e -> {
            StatsChartPanel chartPanel = new StatsChartPanel(loadedMissions);
            JDialog chartDialog = new JDialog(this, "Графики по миссиям", false);
            chartDialog.setLayout(new BorderLayout());
            chartDialog.add(chartPanel, BorderLayout.CENTER);
            chartDialog.pack();
            chartDialog.setLocationRelativeTo(this);
            chartDialog.setVisible(true);
        });

        JPanel btnWrapper = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));
        btnWrapper.setName("chartsBtn");
        btnWrapper.setBackground(AppTheme.bgMain());
        btnWrapper.add(chartsBtn);

        rightPanel.add(btnWrapper, BorderLayout.SOUTH);
        rightPanel.revalidate();
        rightPanel.repaint();
    }

    private void removeChartsButton() {
        JScrollPane reportScroll = (JScrollPane) reportArea.getParent().getParent();
        Container rightPanel = reportScroll.getParent();
        for (Component c : rightPanel.getComponents()) {
            if (c instanceof JPanel p && "chartsBtn".equals(p.getName())) {
                rightPanel.remove(p);
                rightPanel.revalidate();
                rightPanel.repaint();
                return;
            }
        }
    }

    // =========================================================================
    // Плагины и настройки
    // =========================================================================

    public void reloadPlugins() {
        missionService.reloadPlugins();       // пересобирает реестр парсеров
        refreshReportSelector();              // пересобирает комбобокс форматтеров
    }

    /**
     * Первичное заполнение комбобокса форматтеров — при старте приложения.
     * Загружает плагины через PluginManager и добавляет активные форматтеры.
     */
    private void fillReportSelector() {
        List<ReportFormatter> list = ReportFormatterRegistryConfig.createDefault().getAll();
        reportTypeSelector.removeAllItems();
        for (ReportFormatter f : list) reportTypeSelector.addItem(f);
    }

    /**
     * Пересобирает комбобокс форматтеров после изменения состояния плагинов.
     * Сохраняет текущий выбор если соответствующий форматтер остался активным.
     */
    private void refreshReportSelector() {
        // Запоминаем название текущего выбора
        ReportFormatter current = (ReportFormatter) reportTypeSelector.getSelectedItem();
        String currentName = current != null ? current.getDisplayName() : null;

        // Пересобираем список с учётом новых enabled/disabled
        List<ReportFormatter> updated = ReportFormatterRegistryConfig.createDefault().getAll();
        reportTypeSelector.removeAllItems();
        for (ReportFormatter f : updated) reportTypeSelector.addItem(f);

        // Восстанавливаем выбор если он ещё существует, иначе первый элемент
        if (currentName != null) {
            for (int i = 0; i < reportTypeSelector.getItemCount(); i++) {
                if (reportTypeSelector.getItemAt(i).getDisplayName().equals(currentName)) {
                    reportTypeSelector.setSelectedIndex(i);
                    break;
                }
            }
        }

        // Обновляем отчёт под новый форматтер
        if (currentMission != null) refreshReport();
    }

    private void openSettings() {
        new SettingsDialog(this, this::applySettings, () ->
                new PluginsDialog(this, this::reloadPlugins).setVisible(true)
        ).setVisible(true);
    }

    private void applySettings() {
        reportArea.setReportFontSize(SettingsDialog.getFontSize());

        // Смена LAF + обновление дерева компонентов — делегируем в ThemeManager
        ThemeManager.applyLookAndFeel(this, SettingsDialog.isDarkTheme());

        getContentPane().setBackground(AppTheme.bgMain());
        reportArea.setBackground(AppTheme.bgPanel());
        reportArea.setForeground(AppTheme.text());

        ThemeManager.applyThemeToTree(getContentPane());

        // Пересоздаём список миссий под новую тему, восстанавливаем выделение
        missionListPanel.onThemeChanged();
        Mission savedMission = currentMission;
        missionListPanel.rebuild();
        if (savedMission != null) missionListPanel.setSelectedMission(savedMission);

        if (currentMission != null) refreshReport();
        else if (!loadedMissions.isEmpty()) showStats();

        revalidate();
        repaint();
    }

    // =========================================================================
    // Логи
    // =========================================================================

    private void showLogs() {
        JTextPane logPane = new JTextPane();
        logPane.setEditable(false);
        logPane.setFont(new Font("Consolas", Font.PLAIN, 13));
        logPane.setBackground(new Color(30, 32, 36));
        logPane.setMargin(new Insets(12, 12, 12, 12));

        StyledDocument doc = logPane.getStyledDocument();
        for (String line : AppLogger.getLogs().split("\n")) {
            Color color;
            if      (line.startsWith("[ERROR"))  color = new Color(220, 80,  80);
            else if (line.startsWith("[WARN"))   color = new Color(210, 160, 40);
            else if (line.startsWith("[EXPORT")) color = new Color(80,  180, 120);
            else                                 color = new Color(180, 200, 220);

            javax.swing.text.Style style = doc.addStyle(null, null);
            StyleConstants.setForeground(style, color);
            StyleConstants.setFontFamily(style, "Consolas");
            StyleConstants.setFontSize(style, 13);
            try { doc.insertString(doc.getLength(), line + "\n", style); }
            catch (javax.swing.text.BadLocationException ignored) {}
        }
        logPane.setCaretPosition(doc.getLength());

        JScrollPane scrollPane = new JScrollPane(logPane);
        scrollPane.setPreferredSize(new Dimension(700, 400));
        JOptionPane.showMessageDialog(this, scrollPane, "Логи", JOptionPane.PLAIN_MESSAGE);
    }

    // =========================================================================
    // Фабричные методы кнопок
    // =========================================================================

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
        btn.setPreferredSize(new Dimension(btn.getPreferredSize().width, 38));
        return btn;
    }

    private JButton createSecondaryButton(String text) {
        JButton btn = createPrimaryButton(text);
        btn.setBackground(AppTheme.bgSecondary());
        return btn;
    }

    private JButton createSettingsButton(ImageIcon icon) {
        JButton btn = new JButton();
        if (icon != null) btn.setIcon(icon);
        btn.setText("");
        btn.setBackground(AppTheme.bgPanel());
        btn.setOpaque(true);
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(true);
        btn.setBorder(new CompoundBorder(
                new LineBorder(AppTheme.border(), 1, true),
                new EmptyBorder(0, 0, 0, 0)
        ));
        btn.setMargin(new Insets(0, 0, 0, 0));
        btn.setHorizontalAlignment(SwingConstants.CENTER);
        btn.setVerticalAlignment(SwingConstants.CENTER);
        btn.setIconTextGap(0);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(38, 38));
        btn.setToolTipText("Настройки");
        return btn;
    }

    private void styleComboBox(JComboBox<?> comboBox) {
        comboBox.setFont(UI_BOLD);
        comboBox.setPreferredSize(new Dimension(185, 38));
    }

    private void styleScrollPane(JScrollPane sp) {
        sp.getViewport().setBackground(AppTheme.bgPanel());
        sp.setBackground(AppTheme.bgPanel());
        sp.setBorder(new LineBorder(AppTheme.border(), 1, true));
    }

    private javax.swing.border.Border createTitledBorder(String title) {
        return BorderFactory.createTitledBorder(
                new CompoundBorder(
                        new LineBorder(AppTheme.border(), 1, true),
                        new EmptyBorder(6, 6, 6, 6)
                ),
                title, 0, 0, TITLE_FONT, AppTheme.text()
        );
    }

    // =========================================================================
    // Диалоги
    // =========================================================================

    private void warn(String msg)  { JOptionPane.showMessageDialog(this, msg, "Предупреждение", JOptionPane.WARNING_MESSAGE);  }
    private void error(String msg) { JOptionPane.showMessageDialog(this, msg, "Ошибка",          JOptionPane.ERROR_MESSAGE);   }
    private void info(String msg)  { JOptionPane.showMessageDialog(this, msg, "Успех",           JOptionPane.INFORMATION_MESSAGE); }
}
