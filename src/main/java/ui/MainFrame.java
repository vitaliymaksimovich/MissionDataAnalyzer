package ui;

import ai.AiReviewService;
import ai.AiServiceFactory;
import model.Mission;
import service.*;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class MainFrame extends JFrame {

    private final JTextField filePathField;
    private final JTextArea reportArea;
    private final JTextArea aiReviewArea;
    private final JComboBox<ReportFormatter> reportTypeSelector;

    private File selectedFile;
    private Mission currentMission;

    private final MissionService missionService;
    private final AiReviewService aiReviewService;
    private final HtmlReportExporter htmlReportExporter;

    private final List<ReportFormatter> formatters = List.of(
            new FullReportFormatter(),
            new SummaryReportFormatter(),
            new RiskReportFormatter()
    );

    public MainFrame() {
        this.missionService    = new MissionService();
        this.aiReviewService   = AiServiceFactory.create();
        this.htmlReportExporter = new HtmlReportExporter();

        setTitle("Mission Analyzer");
        setSize(900, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // --- верхняя панель ---
        filePathField = new JTextField();
        filePathField.setEditable(false);

        JButton chooseFileButton = new JButton("Выбрать файл");
        JButton analyzeButton    = new JButton("Анализ миссии");
        JButton saveTxtButton    = new JButton("Сохранить TXT");
        JButton saveHtmlButton   = new JButton("Сохранить HTML");

        reportTypeSelector = new JComboBox<>(formatters.toArray(new ReportFormatter[0]));
        reportTypeSelector.setRenderer((list, value, index, isSelected, cellHasFocus) -> {
            JLabel label = new JLabel(value != null ? value.getDisplayName() : "");
            if (isSelected) {
                label.setBackground(list.getSelectionBackground());
                label.setForeground(list.getSelectionForeground());
                label.setOpaque(true);
            }
            return label;
        });
        // при смене типа отчёта — сразу перерисовываем если миссия уже загружена
        reportTypeSelector.addActionListener(e -> refreshReport());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonPanel.add(chooseFileButton);
        buttonPanel.add(analyzeButton);
        buttonPanel.add(new JLabel("Тип отчёта:"));
        buttonPanel.add(reportTypeSelector);
        buttonPanel.add(saveTxtButton);
        buttonPanel.add(saveHtmlButton);

        JPanel topPanel = new JPanel(new BorderLayout(5, 5));
        topPanel.add(new JLabel("Путь к файлу:"), BorderLayout.NORTH);
        topPanel.add(filePathField, BorderLayout.CENTER);
        topPanel.add(buttonPanel, BorderLayout.SOUTH);

        // --- области текста ---
        reportArea = new JTextArea();
        reportArea.setEditable(false);
        reportArea.setLineWrap(true);
        reportArea.setWrapStyleWord(true);

        aiReviewArea = new JTextArea();
        aiReviewArea.setBackground(new Color(235, 248, 235));
        aiReviewArea.setEditable(false);
        aiReviewArea.setLineWrap(true);
        aiReviewArea.setWrapStyleWord(true);
        aiReviewArea.setText("AI-обзор пока недоступен.");

        JPanel reportPanel = new JPanel(new BorderLayout());
        reportPanel.add(new JLabel("Полученные данные"), BorderLayout.NORTH);
        reportPanel.add(new JScrollPane(reportArea), BorderLayout.CENTER);

        JPanel aiPanel = new JPanel(new BorderLayout());
        aiPanel.add(new JLabel("Заключение от GigaChat"), BorderLayout.NORTH);
        aiPanel.add(new JScrollPane(aiReviewArea), BorderLayout.CENTER);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, reportPanel, aiPanel);
        splitPane.setResizeWeight(0.7);
        splitPane.setContinuousLayout(true);
        splitPane.setOneTouchExpandable(true);

        setLayout(new BorderLayout(10, 10));
        add(topPanel, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);

        // --- обработчики ---
        chooseFileButton.addActionListener(e -> chooseFile());
        analyzeButton.addActionListener(e -> analyzeMission());
        saveTxtButton.addActionListener(e -> saveReportToTxt());
        saveHtmlButton.addActionListener(e -> saveReportToHtml());
    }

    private void chooseFile() {
        JFileChooser fc = new JFileChooser();
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            selectedFile = fc.getSelectedFile();
            filePathField.setText(selectedFile.getAbsolutePath());
            currentMission = null;
            reportArea.setText("");
            aiReviewArea.setText("AI-обзор пока недоступен.");
        }
    }

    private void analyzeMission() {
        if (selectedFile == null) {
            warn("Сначала выберите файл миссии.");
            return;
        }
        try {
            currentMission = missionService.loadMission(selectedFile);
            refreshReport();
            aiReviewArea.setText(aiReviewService.generateReview(currentMission));
        } catch (Exception e) {
            error("Ошибка при анализе файла:\n" + e.getMessage());
        }
    }

    // вызывается и при анализе, и при смене типа отчёта
    private void refreshReport() {
        if (currentMission == null) return;
        ReportFormatter formatter = (ReportFormatter) reportTypeSelector.getSelectedItem();
        if (formatter != null) {
            reportArea.setText(formatter.format(currentMission));
            reportArea.setCaretPosition(0);
        }
    }

    private void saveReportToTxt() {
        if (!hasReport()) return;
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Сохранить отчёт");
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File file = ensureExtension(fc.getSelectedFile(), ".txt");
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"));
        String separator = "-".repeat(60);

        String content = "MISSION ANALYZER REPORT\n" + separator + "\n"
                + "Дата анализа: " + timestamp + "\n"
                + "Файл миссии: " + (selectedFile != null ? selectedFile.getName() : "—") + "\n"
                + "Тип отчёта: " + ((ReportFormatter) reportTypeSelector.getSelectedItem()).getDisplayName() + "\n"
                + separator + "\n\n"
                + reportArea.getText() + "\n\n"
                + separator + "\n"
                + "=== ЗАКЛЮЧЕНИЕ ОТ GIGACHAT ===\n"
                + aiReviewArea.getText() + "\n"
                + separator + "\n";

        try (FileWriter fw = new FileWriter(file)) {
            fw.write(content);
            info("Отчёт сохранён:\n" + file.getAbsolutePath());
        } catch (IOException e) {
            error("Не удалось сохранить файл:\n" + e.getMessage());
        }
    }

    private void saveReportToHtml() {
        if (!hasReport()) return;
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Сохранить HTML-отчёт");
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File file = ensureExtension(fc.getSelectedFile(), ".html");
        try {
            htmlReportExporter.export(file, reportArea.getText(), aiReviewArea.getText());
            info("HTML-отчёт сохранён:\n" + file.getAbsolutePath());
        } catch (IOException e) {
            error("Не удалось сохранить HTML-файл:\n" + e.getMessage());
        }
    }

    // --- утилиты ---

    private boolean hasReport() {
        if (reportArea.getText().isBlank()) {
            warn("Сначала выполните анализ миссии.");
            return false;
        }
        return true;
    }

    private File ensureExtension(File file, String ext) {
        return file.getName().toLowerCase().endsWith(ext)
                ? file
                : new File(file.getAbsolutePath() + ext);
    }

    private void warn(String msg)  { JOptionPane.showMessageDialog(this, msg, "Предупреждение", JOptionPane.WARNING_MESSAGE); }
    private void error(String msg) { JOptionPane.showMessageDialog(this, msg, "Ошибка", JOptionPane.ERROR_MESSAGE); }
    private void info(String msg)  { JOptionPane.showMessageDialog(this, msg, "Успех", JOptionPane.INFORMATION_MESSAGE); }
}