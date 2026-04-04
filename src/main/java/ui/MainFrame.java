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
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class MainFrame extends JFrame {

    // --- данные ---
    private final List<Mission> loadedMissions = new ArrayList<>();
    private final List<File> loadedFiles = new ArrayList<>();
    private Mission currentMission;
    private JButton activeButton;
    private JButton statsButton;

    // --- фильтрация ---
    private final FilterChain filterChain = new FilterChain();
    private JTextField searchField;

    // --- сервисы ---
    private final MissionService missionService = new MissionService();
    private final MissionBatchService batchService = new MissionBatchService();
    private final BatchStatsFormatter statsFormatter = new BatchStatsFormatter();
    private final AiReviewService aiReviewService = AiServiceFactory.create();
    private final HtmlReportExporter htmlExporter = new HtmlReportExporter();

    private final List<ReportFormatter> formatters = List.of(
            new FullReportFormatter(),
            new SummaryReportFormatter(),
            new RiskReportFormatter()
    );

    // --- UI ---
    private final StyledReportPane reportArea = new StyledReportPane();
    private final JComboBox<ReportFormatter> reportTypeSelector;
    private final JPanel missionListPanel = new JPanel();

    // gif для помощника
    private ImageIcon assistantGif;
    private ImageIcon assistantStandGif;
    private JButton assistantBtn;

    // --- стили ---
    private static final Color BG_MAIN      = new Color(245, 247, 250);
    private static final Color BG_PANEL     = Color.WHITE;
    private static final Color BG_TOP       = new Color(240, 243, 247);
    private static final Color BG_SELECTED  = new Color(210, 226, 248);
    private static final Color BG_SECONDARY = new Color(233, 239, 247);
    private static final Color BORDER       = new Color(210, 216, 224);
    private static final Color TEXT         = new Color(35, 42, 52);

    private static final Font UI_FONT     = new Font("Segoe UI", Font.PLAIN, 14);
    private static final Font UI_BOLD     = new Font("Segoe UI", Font.BOLD, 14);
    private static final Font TITLE_FONT  = new Font("Segoe UI", Font.BOLD, 15);
    private static final Font REPORT_FONT = new Font("Consolas", Font.PLAIN, 19);

    public MainFrame() {
        reportTypeSelector = new JComboBox<>(formatters.toArray(new ReportFormatter[0]));
        reportTypeSelector.setRenderer((list, value, index, isSelected, cellHasFocus) -> {
            JLabel label = new JLabel(value != null ? value.getDisplayName() : "");
            label.setFont(UI_BOLD);
            label.setBorder(new EmptyBorder(6, 10, 6, 10));
            label.setOpaque(true);
            label.setBackground(isSelected ? list.getSelectionBackground() : Color.WHITE);
            label.setForeground(TEXT);
            return label;
        });
        reportTypeSelector.addActionListener(e -> refreshReport());

        loadAssistantGif();
        setupWindow();
        applySettings();
    }

    private void loadAssistantGif() {
        try {
            java.net.URL sleepUrl = getClass().getResource("/templates/assets/Assistant-sleep.gif");
            if (sleepUrl != null) {
                ImageIcon original = new ImageIcon(sleepUrl);
                Image scaled = original.getImage().getScaledInstance(88, 88, Image.SCALE_DEFAULT);
                assistantGif = new ImageIcon(scaled);
            }
        } catch (Exception ignored) {}

        try {
            java.net.URL standUrl = getClass().getResource("/templates/assets/Assistant-stand.gif");
            if (standUrl != null) {
                ImageIcon original = new ImageIcon(standUrl);
                Image scaled = original.getImage().getScaledInstance(88, 88, Image.SCALE_DEFAULT);
                assistantStandGif = new ImageIcon(scaled);
            }
        } catch (Exception ignored) {}
    }

    private void setupWindow() {
        setTitle("Mission Analyzer");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(8, 8));
        getContentPane().setBackground(BG_MAIN);

        add(buildTopPanel(), BorderLayout.NORTH);
        add(buildCenterPanel(), BorderLayout.CENTER);
    }

    private JPanel buildTopPanel() {
        JButton chooseBtn   = createPrimaryButton("Выбрать файлы");
        JButton saveTxtBtn  = createPrimaryButton("Сохранить TXT");
        JButton saveHtmlBtn = createPrimaryButton("Сохранить HTML");

        ImageIcon settingsIcon = null;
        try {
            java.net.URL url = getClass().getResource("/templates/assets/settings.png");
            if (url != null) {
                ImageIcon original = new ImageIcon(url);
                Image scaled = original.getImage().getScaledInstance(18, 18, Image.SCALE_SMOOTH);
                settingsIcon = new ImageIcon(scaled);
            }
        } catch (Exception ignored) {}

        JButton settingsBtn = createSettingsButton(settingsIcon);

        JButton logsBtn     = createSecondaryButton("Логи");

        styleComboBox(reportTypeSelector);

        chooseBtn.addActionListener(e -> chooseFiles());
        saveTxtBtn.addActionListener(e -> saveReportToTxt());
        saveHtmlBtn.addActionListener(e -> saveReportToHtml());
        settingsBtn.addActionListener(e -> openSettings());
        logsBtn.addActionListener(e -> showLogs());

        JPanel panel = new JPanel(new BorderLayout(12, 0));
        panel.setBackground(BG_TOP);
        panel.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 1, 0, BORDER),
                new EmptyBorder(10, 12, 10, 12)
        ));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);

        JLabel reportTypeLabel = new JLabel("Тип отчёта:");
        reportTypeLabel.setFont(UI_BOLD);
        reportTypeLabel.setForeground(TEXT);

        left.add(chooseBtn);
        left.add(reportTypeLabel);
        left.add(reportTypeSelector);
        left.add(saveTxtBtn);
        left.add(saveHtmlBtn);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        right.setOpaque(false);
        right.add(settingsBtn);
        right.add(logsBtn);

        panel.add(left, BorderLayout.WEST);
        panel.add(right, BorderLayout.EAST);

        return panel;
    }

    private JSplitPane buildCenterPanel() {
        missionListPanel.setLayout(new BoxLayout(missionListPanel, BoxLayout.Y_AXIS));
        missionListPanel.setBackground(BG_PANEL);
        missionListPanel.setBorder(new EmptyBorder(8, 8, 8, 8));

        JScrollPane missionScroll = new JScrollPane(missionListPanel);
        styleScrollPane(missionScroll);
        missionScroll.setBorder(createTitledBorder("Миссии"));
        missionScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        missionScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

        JPanel leftBottomPanel = buildLeftBottomPanel();

        JPanel leftPanel = new JPanel(new BorderLayout(0, 8));
        leftPanel.setBackground(BG_MAIN);
        leftPanel.setBorder(new EmptyBorder(8, 8, 8, 0));
        leftPanel.setPreferredSize(new Dimension(250, 0));
        leftPanel.add(missionScroll, BorderLayout.CENTER);
        leftPanel.add(leftBottomPanel, BorderLayout.SOUTH);

        reportArea.setReportFontFamily("Consolas");
        reportArea.setReportFontSize(SettingsDialog.getFontSize());
        reportArea.setForeground(TEXT);
        reportArea.setBackground(BG_PANEL);
        reportArea.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));

        JScrollPane reportScroll = new JScrollPane(reportArea);
        styleScrollPane(reportScroll);
        reportScroll.setBorder(createTitledBorder("Отчёт"));

        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBackground(BG_MAIN);
        rightPanel.setBorder(new EmptyBorder(8, 0, 8, 8));
        rightPanel.add(reportScroll, BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        split.setDividerLocation(250);
        split.setResizeWeight(0.0);
        split.setBorder(null);
        split.setBackground(BG_MAIN);
        split.setDividerSize(8);

        return split;
    }

    private JPanel buildLeftBottomPanel() {
        // --- панель поиска ---
        JPanel searchPanel = new JPanel();
        searchPanel.setLayout(new BoxLayout(searchPanel, BoxLayout.Y_AXIS));
        searchPanel.setBackground(BG_PANEL);
        searchPanel.setBorder(createTitledBorder("Поиск"));

        searchField = new JTextField();
        searchField.setFont(UI_FONT);
        searchField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        searchField.setToolTipText("Поиск по ID, локации, результату, проклятию");

        String placeholder = "ID, локация, результат, проклятие";
        searchField.setText(placeholder);
        searchField.setForeground(Color.GRAY);

        searchField.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                if (searchField.getText().equals(placeholder)) {
                    searchField.setText("");
                    searchField.setForeground(Color.BLACK);
                }
            }

            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                if (searchField.getText().isEmpty()) {
                    searchField.setText(placeholder);
                    searchField.setForeground(Color.GRAY);
                }
            }
        });

        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e)  { applySearchAndFilter(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e)  { applySearchAndFilter(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { applySearchAndFilter(); }
        });

        JButton filterBtn = createSecondaryButton("Фильтры");
        filterBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        filterBtn.setToolTipText("Настроить фильтры");
        filterBtn.addActionListener(e -> {
            FilterDialog dialog = new FilterDialog(this, filterChain);
            dialog.setVisible(true);
            if (dialog.isApplied()) {
                if (!filterChain.isEmpty()) {
                    filterBtn.setBackground(new Color(180, 210, 255));
                    filterBtn.setOpaque(true);
                } else {
                    filterBtn.setBackground(BG_SECONDARY);
                }
                applySearchAndFilter();
            }
        });

        JPanel searchInner = new JPanel();
        searchInner.setLayout(new BoxLayout(searchInner, BoxLayout.Y_AXIS));
        searchInner.setOpaque(false);
        searchInner.setBorder(new EmptyBorder(6, 6, 6, 6));
        searchInner.add(searchField);
        searchInner.add(Box.createVerticalStrut(6));
        searchInner.add(filterBtn);

        searchPanel.add(searchInner);

        // --- панель действий (статистика + помощник) ---
        JPanel actionsPanel = new JPanel();
        actionsPanel.setLayout(new BoxLayout(actionsPanel, BoxLayout.Y_AXIS));
        actionsPanel.setBackground(BG_PANEL);
        actionsPanel.setBorder(new CompoundBorder(
                new LineBorder(BORDER, 1, true),
                new EmptyBorder(10, 10, 10, 10)
        ));

        statsButton = createSecondaryWideButton("Общая статистика");
        statsButton.addActionListener(e -> {
            if (loadedMissions.isEmpty()) {
                warn("Сначала загрузите файлы миссий.");
                return;
            }
            setActiveButton(statsButton);
            showStats();
        });
        statsButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        assistantBtn = buildAssistantButton();
        assistantBtn.setAlignmentX(Component.CENTER_ALIGNMENT);

        actionsPanel.add(statsButton);
        actionsPanel.add(Box.createVerticalStrut(12));
        actionsPanel.add(assistantBtn);

        // --- общая нижняя панель ---
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BG_MAIN);
        panel.add(searchPanel);
        panel.add(Box.createVerticalStrut(8));
        panel.add(actionsPanel);

        return panel;
    }

    private JButton buildAssistantButton() {
        JButton btn = assistantGif != null
                ? new JButton(assistantGif)
                : new JButton("AI");

        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setToolTipText("Открыть помощника");

        btn.addActionListener(e -> {
            if (currentMission == null) {
                warn("Сначала откройте миссию для анализа.");
                return;
            }
            String reportText = reportArea.getText();
            new AiAssistantDialog(this, aiReviewService, currentMission,
                    assistantStandGif, reportText).show();
        });

        return btn;
    }

    // --- загрузка файлов ---

    private void chooseFiles() {
        JFileChooser fc = new JFileChooser();
        fc.setMultiSelectionEnabled(true);
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        loadedFiles.clear();
        loadedMissions.clear();
        currentMission = null;
        activeButton = null;
        filterChain.clear();
        if (searchField != null) searchField.setText("");
        reportArea.setStyledText("");

        List<String> failed = new ArrayList<>();
        for (File file : fc.getSelectedFiles()) {
            try {
                Mission mission = missionService.loadMission(file);
                loadedFiles.add(file);
                loadedMissions.add(mission);
            } catch (Exception e) {
                AppLogger.error("Не удалось загрузить файл: " + file.getName() + " — " + e.getMessage());
                failed.add(file.getName() + ": " + e.getMessage());
            }
        }

        if (!failed.isEmpty()) {
            warn("Следующие файлы не удалось загрузить:\n" + String.join("\n", failed));
        }

        rebuildMissionList();

        if (!loadedMissions.isEmpty()) {
            currentMission = loadedMissions.get(0);
            refreshReport();
            if (missionListPanel.getComponentCount() > 0) {
                Component first = missionListPanel.getComponent(0);
                if (first instanceof JButton btn) setActiveButton(btn);
            }
        }
    }

    // --- построение списка ---

    private void rebuildMissionList() {
        applySearchAndFilter();
    }

    private void applySearchAndFilter() {
        String query = searchField != null ? searchField.getText() : "";
        if (query.equals("ID, локация, результат, проклятие")) {
            query = "";
        }
        List<Mission> filtered = filterChain.applyWithSearch(loadedMissions, query);
        rebuildMissionListFromFiltered(filtered);
    }

    private void rebuildMissionListFromFiltered(List<Mission> missions) {
        missionListPanel.removeAll();

        for (Mission mission : missions) {
            String label = mission.getMissionId() != null
                    ? mission.getMissionId()
                    : "Миссия";

            JButton btn = createMissionButton(label);
            int realIdx = loadedMissions.indexOf(mission);
            btn.addActionListener(e -> {
                setActiveButton(btn);
                selectMission(realIdx);
            });
            missionListPanel.add(btn);
            missionListPanel.add(Box.createVerticalStrut(8));
        }

        missionListPanel.revalidate();
        missionListPanel.repaint();
    }

    private void setActiveButton(JButton btn) {
        if (activeButton != null) {
            resetMissionButtonStyle(activeButton);
        }
        activeButton = btn;
        activeButton.setBackground(BG_SELECTED);
        activeButton.setBorder(new CompoundBorder(
                new LineBorder(new Color(155, 185, 225), 1, true),
                new EmptyBorder(10, 14, 10, 14)
        ));
        activeButton.setOpaque(true);
    }

    private void selectMission(int idx) {
        currentMission = loadedMissions.get(idx);
        refreshReport();
    }

    private void showStats() {
        if (loadedMissions.isEmpty()) {
            warn("Сначала загрузите файлы миссий.");
            return;
        }
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
        chartsBtn.setForeground(TEXT);
        chartsBtn.setBackground(BG_SECONDARY);
        chartsBtn.setFocusPainted(false);
        chartsBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        chartsBtn.setBorder(new CompoundBorder(
                new LineBorder(BORDER, 1, true),
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
        btnWrapper.setBackground(BG_MAIN);
        btnWrapper.add(chartsBtn);

        rightPanel.add(btnWrapper, BorderLayout.SOUTH);
        rightPanel.revalidate();
        rightPanel.repaint();
    }

    private void refreshReport() {
        if (currentMission == null) return;
        ReportFormatter formatter = (ReportFormatter) reportTypeSelector.getSelectedItem();
        if (formatter == null) return;
        reportArea.setStyledText(formatter.format(currentMission));
        removeChartsButton();
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

    // --- настройки ---

    private void openSettings() {
        new SettingsDialog(this, this::applySettings).setVisible(true);
    }

    private void applySettings() {
        reportArea.setReportFontSize(SettingsDialog.getFontSize());

        if (SettingsDialog.isDarkTheme()) {
            Color darkMain = new Color(40, 43, 48);
            Color darkPanel = new Color(30, 32, 36);
            Color darkText = new Color(200, 210, 220);

            getContentPane().setBackground(darkMain);
            missionListPanel.setBackground(darkPanel);
            reportArea.setBackground(darkPanel);
            reportArea.setForeground(darkText);

        } else {
            getContentPane().setBackground(BG_MAIN);
            missionListPanel.setBackground(BG_PANEL);
            reportArea.setBackground(BG_PANEL);
            reportArea.setForeground(TEXT);
        }

        if (currentMission != null) {
            refreshReport();
        } else if (!loadedMissions.isEmpty()) {
            showStats();
        }

        revalidate();
        repaint();
    }

    // --- логи ---

    private void showLogs() {
        JTextPane logPane = new JTextPane();
        logPane.setEditable(false);
        logPane.setFont(new Font("Consolas", Font.PLAIN, 13));
        logPane.setBackground(new Color(30, 32, 36));
        logPane.setMargin(new Insets(12, 12, 12, 12));

        StyledDocument doc = logPane.getStyledDocument();
        String[] lines = AppLogger.getLogs().split("\n");
        for (String line : lines) {
            Color color;
            if (line.startsWith("[ERROR"))        color = new Color(220, 80, 80);
            else if (line.startsWith("[WARN"))    color = new Color(210, 160, 40);
            else if (line.startsWith("[EXPORT"))  color = new Color(80, 180, 120);
            else                                  color = new Color(180, 200, 220);

            javax.swing.text.Style style = doc.addStyle(null, null);
            StyleConstants.setForeground(style, color);
            StyleConstants.setFontFamily(style, "Consolas");
            StyleConstants.setFontSize(style, 13);
            try {
                doc.insertString(doc.getLength(), line + "\n", style);
            } catch (javax.swing.text.BadLocationException ignored) {}
        }

        logPane.setCaretPosition(doc.getLength());

        JScrollPane scrollPane = new JScrollPane(logPane);
        scrollPane.setPreferredSize(new Dimension(700, 400));

        JOptionPane.showMessageDialog(this, scrollPane, "Логи", JOptionPane.PLAIN_MESSAGE);
    }

    // --- экспорт ---

    private void saveReportToTxt() {
        if (reportArea.getText().isBlank()) {
            warn("Сначала выполните анализ.");
            return;
        }
        JFileChooser fc = new JFileChooser();
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File file = ensureExtension(fc.getSelectedFile(), ".txt");

        String sep = "-".repeat(60);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"));
        ReportFormatter fmt = (ReportFormatter) reportTypeSelector.getSelectedItem();

        String content = "MISSION ANALYZER REPORT\n" + sep + "\n"
                + "Дата анализа: " + timestamp + "\n"
                + "Тип отчёта:   " + (fmt != null ? fmt.getDisplayName() : "—") + "\n"
                + sep + "\n\n"
                + reportArea.getText() + "\n"
                + sep + "\n";

        try (FileWriter fw = new FileWriter(file)) {
            fw.write(content);
            AppLogger.export("TXT", file.getAbsolutePath());
            info("Отчёт сохранён:\n" + file.getAbsolutePath());
        } catch (IOException e) {
            AppLogger.error("Не удалось сохранить TXT: " + e.getMessage());
            error("Не удалось сохранить:\n" + e.getMessage());
        }
    }

    private void saveReportToHtml() {
        if (reportArea.getText().isBlank()) {
            warn("Сначала выполните анализ.");
            return;
        }
        JFileChooser fc = new JFileChooser();
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File file = ensureExtension(fc.getSelectedFile(), ".html");
        try {
            htmlExporter.export(file, reportArea.getText());
            AppLogger.export("HTML", file.getAbsolutePath());
            info("HTML сохранён:\n" + file.getAbsolutePath());
        } catch (IOException e) {
            AppLogger.error("Не удалось сохранить HTML: " + e.getMessage());
            error("Не удалось сохранить:\n" + e.getMessage());
        }
    }

    private File ensureExtension(File file, String ext) {
        return file.getName().toLowerCase().endsWith(ext)
                ? file
                : new File(file.getAbsolutePath() + ext);
    }

    // --- фабричные методы кнопок ---

    private JButton createPrimaryButton(String text) {
        JButton button = new JButton(text);
        button.setFont(UI_BOLD);
        button.setForeground(TEXT);
        button.setBackground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(new CompoundBorder(
                new LineBorder(BORDER, 1, true),
                new EmptyBorder(8, 16, 8, 16)
        ));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(button.getPreferredSize().width, 38));
        return button;
    }

    private JButton createSecondaryButton(String text) {
        JButton button = createPrimaryButton(text);
        button.setBackground(BG_SECONDARY);
        return button;
    }

    private JButton createSettingsButton(ImageIcon icon) {
        JButton button = new JButton();

        if (icon != null) {
            button.setIcon(icon);
        }

        button.setText("");

        button.setBackground(Color.WHITE);
        button.setOpaque(true);
        button.setFocusPainted(false);
        button.setContentAreaFilled(true);

        button.setBorder(new CompoundBorder(
                new LineBorder(BORDER, 1, true),
                new EmptyBorder(0, 0, 0, 0)
        ));

        button.setMargin(new Insets(0, 0, 0, 0));
        button.setHorizontalAlignment(SwingConstants.CENTER);
        button.setVerticalAlignment(SwingConstants.CENTER);
        button.setIconTextGap(0);

        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(38, 38));
        button.setToolTipText("Настройки");

        return button;
    }

    private JButton createMissionButton(String text) {
        JButton button = new JButton(text);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setFont(UI_BOLD);
        button.setForeground(TEXT);
        button.setBackground(Color.WHITE);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(new CompoundBorder(
                new LineBorder(BORDER, 1, true),
                new EmptyBorder(10, 14, 10, 14)
        ));
        return button;
    }

    private JButton createSecondaryWideButton(String text) {
        JButton button = createSecondaryButton(text);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        return button;
    }

    private void resetMissionButtonStyle(JButton button) {
        button.setBackground(Color.WHITE);
        button.setBorder(new CompoundBorder(
                new LineBorder(BORDER, 1, true),
                new EmptyBorder(10, 14, 10, 14)
        ));
        button.setOpaque(true);
    }

    private void styleComboBox(JComboBox<?> comboBox) {
        comboBox.setFont(UI_BOLD);
        comboBox.setBackground(Color.WHITE);
        comboBox.setForeground(TEXT);
        comboBox.setBorder(new LineBorder(BORDER, 1, true));
        comboBox.setPreferredSize(new Dimension(185, 38));

        Component editor = comboBox.getEditor().getEditorComponent();
        if (editor != null) {
            editor.setFont(UI_BOLD);
        }
    }

    private void styleScrollPane(JScrollPane scrollPane) {
        scrollPane.getViewport().setBackground(BG_PANEL);
        scrollPane.setBackground(BG_PANEL);
        scrollPane.setBorder(new LineBorder(BORDER, 1, true));
    }

    private javax.swing.border.Border createTitledBorder(String title) {
        return BorderFactory.createTitledBorder(
                new CompoundBorder(
                        new LineBorder(BORDER, 1, true),
                        new EmptyBorder(6, 6, 6, 6)
                ),
                title,
                0,
                0,
                TITLE_FONT,
                TEXT
        );
    }

    private void warn(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Предупреждение", JOptionPane.WARNING_MESSAGE);
    }

    private void error(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Ошибка", JOptionPane.ERROR_MESSAGE);
    }

    private void info(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Успех", JOptionPane.INFORMATION_MESSAGE);
    }
}