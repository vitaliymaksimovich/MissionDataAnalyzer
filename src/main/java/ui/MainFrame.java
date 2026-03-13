package ui;

import model.Mission;
import service.MissionReportFormatter;
import service.MissionService;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class MainFrame extends JFrame {

    private final JTextField filePathField;
    private final JTextArea reportArea;
    private final JTextArea aiReviewArea;

    private File selectedFile;

    private final MissionService missionService;
    private final MissionReportFormatter reportFormatter;

    public MainFrame() {
        this.missionService = new MissionService();
        this.reportFormatter = new MissionReportFormatter();

        setTitle("Mission Analyzer");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel topPanel = new JPanel(new BorderLayout(5, 5));
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        filePathField = new JTextField();
        filePathField.setEditable(false);

        JButton chooseFileButton = new JButton("Выбрать файл");
        JButton analyzeButton = new JButton("Анализ миссии");

        buttonPanel.add(chooseFileButton);
        buttonPanel.add(analyzeButton);

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
        aiReviewArea.setText("AI-обзор пока не реализован.");

        JScrollPane reportScrollPane = new JScrollPane(reportArea);
        JScrollPane aiScrollPane = new JScrollPane(aiReviewArea);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, reportScrollPane, aiScrollPane);
        splitPane.setResizeWeight(0.75);

        setLayout(new BorderLayout(10, 10));
        add(topPanel, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);

        chooseFileButton.addActionListener(e -> chooseFile());
        analyzeButton.addActionListener(e -> analyzeMission());
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

            reportArea.setText(report);
            aiReviewArea.setText("AI-обзор пока не реализован.");

        } catch (Exception e) {
            JOptionPane.showMessageDialog(
                    this,
                    "Ошибка при анализе файла:\n" + e.getMessage(),
                    "Ошибка",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }
}