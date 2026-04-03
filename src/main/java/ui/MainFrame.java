package ui;

import ai.AiReviewService;
import ai.AiServiceFactory;
import model.Mission;
import service.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
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
    private JButton activeButton = null; // текущая выделенная кнопка

    // --- сервисы ---
    private final MissionBatchService batchService = new MissionBatchService();
    private final BatchStatsFormatter statsFormatter = new BatchStatsFormatter();
    private final MissionService missionService = new MissionService();
    private final AiReviewService aiReviewService = AiServiceFactory.create();
    private final HtmlReportExporter htmlReportExporter = new HtmlReportExporter();

    private final List<ReportFormatter> formatters = List.of(
            new FullReportFormatter(),
            new SummaryReportFormatter(),
            new RiskReportFormatter()
    );

    // --- UI ---
    private final JTextArea reportArea = new JTextArea();
    private final JTextArea aiReviewArea = new JTextArea();
    private final JComboBox<ReportFormatter> reportTypeSelector;
    private final JPanel missionListPanel = new JPanel();

    public MainFrame() {
        reportTypeSelector = new JComboBox<>(formatters.toArray(new ReportFormatter[0]));
        reportTypeSelector.setRenderer((list, value, index, isSelected, cellHasFocus) -> {
            JLabel label = new JLabel(value != null ? value.getDisplayName() : "");
            if (isSelected) { label.setOpaque(true); label.setBackground(list.getSelectionBackground()); }
            return label;
        });
        reportTypeSelector.addActionListener(e -> refreshReport());
        setupWindow();
    }

    private void setupWindow() {
        setTitle("Mission Analyzer");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(5, 5));
        add(buildTopPanel(), BorderLayout.NORTH);
        add(buildCenterPanel(), BorderLayout.CENTER);
        add(buildAiPanel(), BorderLayout.SOUTH);
    }

    private JPanel buildTopPanel() {
        JButton chooseBtn   = new JButton("Выбрать файлы");
        JButton saveTxtBtn  = new JButton("Сохранить TXT");
        JButton saveHtmlBtn = new JButton("Сохранить HTML");
        JButton logsBtn     = new JButton("Логи");

        chooseBtn.addActionListener(e -> chooseFiles());
        saveTxtBtn.addActionListener(e -> saveReportToTxt());
        saveHtmlBtn.addActionListener(e -> saveReportToHtml());
        logsBtn.addActionListener(e -> showLogs());

        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        panel.add(chooseBtn);
        panel.add(new JLabel("Тип отчёта:"));
        panel.add(reportTypeSelector);
        panel.add(saveTxtBtn);
        panel.add(saveHtmlBtn);
        panel.add(logsBtn);
        return panel;
    }

    private JSplitPane buildCenterPanel() {
        missionListPanel.setLayout(new BoxLayout(missionListPanel, BoxLayout.Y_AXIS));
        missionListPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

        JScrollPane listScroll = new JScrollPane(missionListPanel);
        listScroll.setPreferredSize(new Dimension(200, 0));
        listScroll.setBorder(BorderFactory.createTitledBorder("Миссии"));

        reportArea.setEditable(false);
        reportArea.setLineWrap(true);
        reportArea.setWrapStyleWord(true);
        reportArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));

        JScrollPane reportScroll = new JScrollPane(reportArea);
        reportScroll.setBorder(BorderFactory.createTitledBorder("Отчёт"));

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, listScroll, reportScroll);
        split.setDividerLocation(200);
        split.setResizeWeight(0.0);
        return split;
    }

    private JPanel buildAiPanel() {
        aiReviewArea.setEditable(false);
        aiReviewArea.setLineWrap(true);
        aiReviewArea.setWrapStyleWord(true);
        aiReviewArea.setBackground(new Color(235, 248, 235));
        aiReviewArea.setRows(4);
        aiReviewArea.setText("AI-обзор пока недоступен.");

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JLabel("Заключение от GigaChat"), BorderLayout.NORTH);
        panel.add(new JScrollPane(aiReviewArea), BorderLayout.CENTER);
        panel.setPreferredSize(new Dimension(0, 130));
        return panel;
    }

    // --- загрузка файлов с валидацией на месте ---
    private void chooseFiles() {
        JFileChooser fc = new JFileChooser();
        fc.setMultiSelectionEnabled(true);
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        loadedFiles.clear();
        loadedMissions.clear();
        currentMission = null;
        activeButton = null;
        reportArea.setText("");
        aiReviewArea.setText("AI-обзор пока недоступен.");

        List<String> failed = new ArrayList<>();
        for (File file : fc.getSelectedFiles()) {
            // проверяем файл ещё на стадии загрузки — пробуем распарсить
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

        // сразу показываем первую миссию если есть
        if (!loadedMissions.isEmpty()) {
            currentMission = loadedMissions.get(0);
            refreshReport();
            // первую кнопку выделяем
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

            JButton btn = new JButton(label);
            btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
            btn.setHorizontalAlignment(SwingConstants.LEFT);

            final int idx = i;
            btn.addActionListener(e -> {
                setActiveButton(btn);
                selectMission(idx);
            });
            missionListPanel.add(btn);
            missionListPanel.add(Box.createVerticalStrut(3));
        }

        // кнопка "Общая статистика" внизу списка — только если есть хоть одна миссия
        if (!loadedMissions.isEmpty()) {
            missionListPanel.add(Box.createVerticalStrut(10));
            JSeparator sep = new JSeparator();
            sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 2));
            missionListPanel.add(sep);
            missionListPanel.add(Box.createVerticalStrut(5));

            JButton statsBtn = new JButton("Общая статистика");
            statsBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
            statsBtn.setBackground(new Color(220, 235, 255));
            statsBtn.addActionListener(e -> {
                setActiveButton(statsBtn);
                showStats();
            });
            missionListPanel.add(statsBtn);
        }

        missionListPanel.revalidate();
        missionListPanel.repaint();
    }

    // выделяем нажатую кнопку, снимаем с предыдущей
    private void setActiveButton(JButton btn) {
        if (activeButton != null) {
            activeButton.setBackground(null);
            activeButton.setOpaque(false);
        }
        activeButton = btn;
        activeButton.setBackground(new Color(180, 210, 255));
        activeButton.setOpaque(true);
    }

    private void selectMission(int idx) {
        currentMission = loadedMissions.get(idx);
        refreshReport();
        aiReviewArea.setText(aiReviewService.generateReview(currentMission));
    }

    private void showStats() {
        currentMission = null;
        reportArea.setText(statsFormatter.format(loadedMissions));
        reportArea.setCaretPosition(0);
        aiReviewArea.setText("AI-обзор недоступен для общей статистики.");
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
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        logArea.setText(AppLogger.getLogs());
        logArea.setCaretPosition(logArea.getDocument().getLength());
        JOptionPane.showMessageDialog(this, new JScrollPane(logArea), "Логи", JOptionPane.INFORMATION_MESSAGE);
    }

    private void saveReportToTxt() {
        if (reportArea.getText().isBlank()) { warn("Сначала выполните анализ."); return; }
        JFileChooser fc = new JFileChooser();
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File file = ensureExtension(fc.getSelectedFile(), ".txt");

        String sep = "-".repeat(60);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"));
        ReportFormatter fmt = (ReportFormatter) reportTypeSelector.getSelectedItem();

        String content = "MISSION ANALYZER REPORT\n" + sep + "\n"
                + "Дата анализа: " + timestamp + "\n"
                + "Тип отчёта: " + (fmt != null ? fmt.getDisplayName() : "—") + "\n"
                + sep + "\n\n"
                + reportArea.getText() + "\n\n"
                + sep + "\n=== ЗАКЛЮЧЕНИЕ ОТ GIGACHAT ===\n"
                + aiReviewArea.getText() + "\n" + sep + "\n";

        try (FileWriter fw = new FileWriter(file)) {
            fw.write(content);
            info("Отчёт сохранён:\n" + file.getAbsolutePath());
        } catch (IOException e) {
            error("Не удалось сохранить:\n" + e.getMessage());
        }
    }

    private void saveReportToHtml() {
        if (reportArea.getText().isBlank()) { warn("Сначала выполните анализ."); return; }
        JFileChooser fc = new JFileChooser();
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File file = ensureExtension(fc.getSelectedFile(), ".html");
        try {
            htmlReportExporter.export(file, reportArea.getText(), aiReviewArea.getText());
            info("HTML сохранён:\n" + file.getAbsolutePath());
        } catch (IOException e) {
            error("Не удалось сохранить:\n" + e.getMessage());
        }
    }

    private File ensureExtension(File file, String ext) {
        return file.getName().toLowerCase().endsWith(ext) ? file : new File(file.getAbsolutePath() + ext);
    }
    private void warn(String msg)  { JOptionPane.showMessageDialog(this, msg, "Предупреждение", JOptionPane.WARNING_MESSAGE); }
    private void error(String msg) { JOptionPane.showMessageDialog(this, msg, "Ошибка", JOptionPane.ERROR_MESSAGE); }
    private void info(String msg)  { JOptionPane.showMessageDialog(this, msg, "Успех", JOptionPane.INFORMATION_MESSAGE); }
}