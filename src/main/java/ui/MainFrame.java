package ui;

import ai.AiReviewService;
import ai.AiServiceFactory;
import model.Mission;
import service.MissionReportFormatter;
import service.MissionService;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class MainFrame extends JFrame {

    private final JTextField filePathField;
    private final JTextArea reportArea;
    private final JTextArea aiReviewArea;

    private File selectedFile;

    private final MissionService missionService;
    private final MissionReportFormatter reportFormatter;
    private final AiReviewService aiReviewService;

    public MainFrame() {
        this.missionService = new MissionService();
        this.reportFormatter = new MissionReportFormatter();
        this.aiReviewService = AiServiceFactory.create();

        setTitle("Mission Analyzer");
        setSize(900, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel topPanel = new JPanel(new BorderLayout(5, 5));
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        filePathField = new JTextField();
        filePathField.setEditable(false);

        JButton chooseFileButton = new JButton("Выбрать файл");
        JButton analyzeButton = new JButton("Анализ миссии");
        JButton saveButton = new JButton("Сохранить отчёт в TXT");

        buttonPanel.add(chooseFileButton);
        buttonPanel.add(analyzeButton);
        buttonPanel.add(saveButton);

        topPanel.add(new JLabel("Путь к файлу:"), BorderLayout.NORTH);
        topPanel.add(filePathField, BorderLayout.CENTER);
        topPanel.add(buttonPanel, BorderLayout.SOUTH);

        reportArea = new JTextArea();
        reportArea.setEditable(false);
        reportArea.setLineWrap(true);
        reportArea.setWrapStyleWord(true);

        aiReviewArea = new JTextArea();
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
        splitPane.setDividerLocation(400);

        setLayout(new BorderLayout(10, 10));
        add(topPanel, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);

        chooseFileButton.addActionListener(e -> chooseFile());
        analyzeButton.addActionListener(e -> analyzeMission());
        saveButton.addActionListener(e -> saveReportToTxt());
    }

    private void chooseFile() {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            selectedFile = fileChooser.getSelectedFile();
            filePathField.setText(selectedFile.getAbsolutePath());
        }
    }

    private void analyzeMission() {
        if (selectedFile == null) {
            JOptionPane.showMessageDialog(
                    this,
                    "Сначала выберите файл миссии.",
                    "Ошибка",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        try {
            Mission mission = missionService.loadMission(selectedFile);

            String report = reportFormatter.format(mission);
            String aiReview = aiReviewService.generateReview(mission);

            reportArea.setText(report);
            aiReviewArea.setText(aiReview);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(
                    this,
                    "Ошибка при анализе файла:\n" + e.getMessage(),
                    "Ошибка",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void saveReportToTxt() {
        if (reportArea.getText().isBlank()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Сначала выполните анализ миссии.",
                    "Предупреждение",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Сохранить отчёт");

        int result = fileChooser.showSaveDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();

            if (!file.getName().toLowerCase().endsWith(".txt")) {
                file = new File(file.getAbsolutePath() + ".txt");
            }

            String analysisDateTime = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"));

            String sourceFileName = selectedFile != null ? selectedFile.getName() : "Неизвестно";

            String separator = "------------------------------------------------------------";

            String content =
                    "MISSION ANALYZER REPORT\n" +
                            separator + "\n" +
                            "Дата и время анализа: " + analysisDateTime + "\n" +
                            "Исходный файл миссии: " + sourceFileName + "\n" +
                            separator + "\n\n" +
                            "=== ПОЛУЧЕННЫЕ ДАННЫЕ ===\n" +
                            reportArea.getText() +
                            "\n\n" +
                            separator + "\n" +
                            "=== ЗАКЛЮЧЕНИЕ ОТ GIGACHAT ===\n" +
                            aiReviewArea.getText() +
                            "\n" +
                            separator + "\n";

            try (FileWriter writer = new FileWriter(file)) {
                writer.write(content);

                JOptionPane.showMessageDialog(
                        this,
                        "Отчёт успешно сохранён:\n" + file.getAbsolutePath(),
                        "Успех",
                        JOptionPane.INFORMATION_MESSAGE
                );

            } catch (IOException e) {
                JOptionPane.showMessageDialog(
                        this,
                        "Не удалось сохранить файл:\n" + e.getMessage(),
                        "Ошибка",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        }
    }
}