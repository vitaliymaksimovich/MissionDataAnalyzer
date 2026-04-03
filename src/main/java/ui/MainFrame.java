package ui;

import ai.AiReviewService;
import ai.AiServiceFactory;
import model.Mission;
import service.*;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
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
    private final JTextArea reportArea = new JTextArea();
    private final JComboBox<ReportFormatter> reportTypeSelector;
    private final JPanel missionListPanel = new JPanel();

    // gif для помощника
    private ImageIcon assistantGif;
    private ImageIcon assistantStandGif;
    private JButton assistantBtn;

    // --- стили ---
    private static final Color BG_MAIN = new Color(245, 247, 250);
    private static final Color BG_PANEL = Color.WHITE;
    private static final Color BG_TOP = new Color(240, 243, 247);
    private static final Color BG_SELECTED = new Color(210, 226, 248);
    private static final Color BG_SECONDARY = new Color(233, 239, 247);
    private static final Color BORDER = new Color(210, 216, 224);
    private static final Color TEXT = new Color(35, 42, 52);

    private static final Font UI_FONT = new Font("Segoe UI", Font.PLAIN, 14);
    private static final Font UI_BOLD = new Font("Segoe UI", Font.BOLD, 14);
    private static final Font TITLE_FONT = new Font("Segoe UI", Font.BOLD, 15);
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
    }

    private void loadAssistantGif() {
        try {
            java.net.URL sleepUrl = getClass().getResource("/templates/assets/Assistant-sleep.gif");
            if (sleepUrl != null) {
                ImageIcon original = new ImageIcon(sleepUrl);
                Image scaled = original.getImage().getScaledInstance(88, 88, Image.SCALE_DEFAULT);
                assistantGif = new ImageIcon(scaled);
            }
        } catch (Exception ignored) {
        }

        try {
            java.net.URL standUrl = getClass().getResource("/templates/assets/Assistant-stand.gif");
            if (standUrl != null) {
                ImageIcon original = new ImageIcon(standUrl);
                Image scaled = original.getImage().getScaledInstance(88, 88, Image.SCALE_DEFAULT);
                assistantStandGif = new ImageIcon(scaled);
            }
        } catch (Exception ignored) {
        }
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
        JButton chooseBtn = createPrimaryButton("Выбрать файлы");
        JButton saveTxtBtn = createPrimaryButton("Сохранить TXT");
        JButton saveHtmlBtn = createPrimaryButton("Сохранить HTML");
        JButton logsBtn = createSecondaryButton("Логи");

        styleComboBox(reportTypeSelector);

        chooseBtn.addActionListener(e -> chooseFiles());
        saveTxtBtn.addActionListener(e -> saveReportToTxt());
        saveHtmlBtn.addActionListener(e -> saveReportToHtml());
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

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        right.setOpaque(false);
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

        reportArea.setEditable(false);
        reportArea.setLineWrap(true);
        reportArea.setWrapStyleWord(true);
        reportArea.setFont(REPORT_FONT);
        reportArea.setForeground(TEXT);
        reportArea.setBackground(BG_PANEL);
        reportArea.setMargin(new Insets(18, 20, 18, 20));
        reportArea.setBorder(null);
        reportArea.setCaretColor(TEXT);

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
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BG_PANEL);
        panel.setBorder(new CompoundBorder(
                new LineBorder(BORDER, 1, true),
                new EmptyBorder(10, 10, 10, 10)
        ));

        statsButton = createSecondaryWideButton("Общая статистика");
        statsButton.addActionListener(e -> {
            setActiveButton(statsButton);
            showStats();
        });
        statsButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        assistantBtn = buildAssistantButton();
        assistantBtn.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(statsButton);
        panel.add(Box.createVerticalStrut(12));
        panel.add(assistantBtn);

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
            new AiAssistantDialog(this, aiReviewService, currentMission, assistantStandGif, reportText).show();
        });

        return btn;
    }

    private void chooseFiles() {
        JFileChooser fc = new JFileChooser();
        fc.setMultiSelectionEnabled(true);
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        loadedFiles.clear();
        loadedMissions.clear();
        currentMission = null;
        activeButton = null;
        reportArea.setText("");

        List<String> failed = new ArrayList<>();
        for (File file : fc.getSelectedFiles()) {
            try {
                Mission mission = missionService.loadMission(file);
                loadedFiles.add(file);
                loadedMissions.add(mission);
            } catch (Exception e) {
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

    private void rebuildMissionList() {
        missionListPanel.removeAll();

        for (int i = 0; i < loadedMissions.size(); i++) {
            Mission mission = loadedMissions.get(i);
            String label = mission.getMissionId() != null
                    ? mission.getMissionId()
                    : loadedFiles.get(i).getName();

            JButton btn = createMissionButton(label);

            final int idx = i;
            btn.addActionListener(e -> {
                setActiveButton(btn);
                selectMission(idx);
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
        currentMission = null;
        reportArea.setText(statsFormatter.format(loadedMissions));
        reportArea.setCaretPosition(0);
    }

    private void refreshReport() {
        if (currentMission == null) return;
        ReportFormatter formatter = (ReportFormatter) reportTypeSelector.getSelectedItem();
        if (formatter == null) return;
        reportArea.setText(formatter.format(currentMission));
        reportArea.setCaretPosition(0);
    }

    private void showLogs() {
        JTextArea logArea = new JTextArea(25, 70);
        logArea.setEditable(false);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        logArea.setText(service.AppLogger.getLogs());
        logArea.setCaretPosition(logArea.getDocument().getLength());
        logArea.setMargin(new Insets(12, 12, 12, 12));

        JScrollPane scrollPane = new JScrollPane(logArea);
        styleScrollPane(scrollPane);

        JOptionPane.showMessageDialog(this, scrollPane, "Логи", JOptionPane.INFORMATION_MESSAGE);
    }

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
            info("Отчёт сохранён:\n" + file.getAbsolutePath());
        } catch (IOException e) {
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
            info("HTML сохранён:\n" + file.getAbsolutePath());
        } catch (IOException e) {
            error("Не удалось сохранить:\n" + e.getMessage());
        }
    }

    private File ensureExtension(File file, String ext) {
        return file.getName().toLowerCase().endsWith(ext)
                ? file
                : new File(file.getAbsolutePath() + ext);
    }

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