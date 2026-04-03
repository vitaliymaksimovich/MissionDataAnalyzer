package ui;

import ai.AiReviewService;
import model.Mission;
import service.HtmlReportExporter;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

public class AiAssistantDialog {

    private final JFrame parent;
    private final AiReviewService aiService;
    private final Mission mission;
    private final ImageIcon gifIcon;
    private final HtmlReportExporter htmlExporter = new HtmlReportExporter();
    private final String reportText;

    private final Map<String, Function<Mission, String>> options = new LinkedHashMap<>();

    public AiAssistantDialog(JFrame parent, AiReviewService aiService,
                             Mission mission, ImageIcon gifIcon, String reportText) {
        this.parent = parent;
        this.aiService = aiService;
        this.mission = mission;
        this.gifIcon = gifIcon;
        this.reportText = reportText;

        options.put("Краткий анализ",     aiService::generateBriefAnalysis);
        options.put("Подробный анализ",   aiService::generateDetailedAnalysis);
        options.put("Поиск проблем",      aiService::findProblems);
        options.put("Рекомендации",       aiService::generateRecommendations);
        options.put("История по мотивам", aiService::generateStory);
    }

    public void show() {
        showSelectionDialog();
    }

    // --- попап 1: выбор ---

    private void showSelectionDialog() {
        JDialog dialog = new JDialog(parent, "Помощник GigaChat", true);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.setSize(360, 400);
        dialog.setLocationRelativeTo(parent);
        dialog.setResizable(false);

        JPanel topPanel = new JPanel(new BorderLayout(5, 8));
        topPanel.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));

        if (gifIcon != null) {
            JLabel gifLabel = new JLabel(gifIcon, SwingConstants.CENTER);
            topPanel.add(gifLabel, BorderLayout.CENTER);
        }

        JLabel questionLabel = new JLabel("Что вас интересует?", SwingConstants.CENTER);
        questionLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        topPanel.add(questionLabel, BorderLayout.SOUTH);

        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.Y_AXIS));
        buttonsPanel.setBorder(BorderFactory.createEmptyBorder(8, 24, 4, 24));

        for (Map.Entry<String, Function<Mission, String>> entry : options.entrySet()) {
            JButton btn = new JButton(entry.getKey());
            btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
            btn.setAlignmentX(Component.CENTER_ALIGNMENT);
            btn.addActionListener(e -> {
                dialog.dispose();
                showLoadingThenResult(entry.getKey(), entry.getValue());
            });
            buttonsPanel.add(btn);
            buttonsPanel.add(Box.createVerticalStrut(5));
        }

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton cancelBtn = new JButton("Отмена");
        cancelBtn.addActionListener(e -> dialog.dispose());
        bottomPanel.add(cancelBtn);

        dialog.add(topPanel, BorderLayout.NORTH);
        dialog.add(buttonsPanel, BorderLayout.CENTER);
        dialog.add(bottomPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    // --- загрузка ---

    private void showLoadingThenResult(String title, Function<Mission, String> action) {
        JDialog loadingDialog = new JDialog(parent, "GigaChat думает...", true);
        loadingDialog.setLayout(new BorderLayout(10, 10));
        loadingDialog.setSize(320, 110);
        loadingDialog.setLocationRelativeTo(parent);
        loadingDialog.setResizable(false);
        loadingDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

        JLabel label = new JLabel("Запрос отправлен, ожидаем ответ...", SwingConstants.CENTER);
        label.setFont(new Font("SansSerif", Font.PLAIN, 13));

        JProgressBar progress = new JProgressBar();
        progress.setIndeterminate(true);
        progress.setBorder(BorderFactory.createEmptyBorder(0, 16, 8, 16));

        loadingDialog.add(label, BorderLayout.CENTER);
        loadingDialog.add(progress, BorderLayout.SOUTH);

        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() {
                return action.apply(mission);
            }

            @Override
            protected void done() {
                loadingDialog.dispose();
                try {
                    showResultDialog(title, get());
                } catch (Exception e) {
                    showResultDialog(title, "Ошибка: " + e.getMessage());
                }
            }
        };

        worker.execute();
        loadingDialog.setVisible(true);
    }

    // --- попап 2: результат ---

    private void showResultDialog(String title, String result) {
        JDialog dialog = new JDialog(parent, "GigaChat — " + title, true);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.setSize(580, 440);
        dialog.setLocationRelativeTo(parent);

        JTextArea resultArea = new JTextArea(result);
        resultArea.setEditable(false);
        resultArea.setLineWrap(true);
        resultArea.setWrapStyleWord(true);
        resultArea.setFont(new Font("SansSerif", Font.PLAIN, 13));
        resultArea.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JScrollPane scroll = new JScrollPane(resultArea);
        scroll.setBorder(BorderFactory.createTitledBorder(title));

        JButton closeBtn = new JButton("Закрыть");
        JButton copyBtn  = new JButton("Скопировать");
        JButton txtBtn   = new JButton("Экспорт TXT + AI");
        JButton htmlBtn  = new JButton("Экспорт HTML + AI");

        closeBtn.addActionListener(e -> dialog.dispose());

        copyBtn.addActionListener(e -> {
            Toolkit.getDefaultToolkit()
                    .getSystemClipboard()
                    .setContents(new StringSelection(result), null);
            JOptionPane.showMessageDialog(dialog, "Скопировано!", "Успех",
                    JOptionPane.INFORMATION_MESSAGE);
        });

        txtBtn.addActionListener(e -> exportTxt(result, title, dialog));
        htmlBtn.addActionListener(e -> exportHtml(result, title, dialog));

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 8));
        btnPanel.add(closeBtn);
        btnPanel.add(copyBtn);
        btnPanel.add(txtBtn);
        btnPanel.add(htmlBtn);

        dialog.add(scroll, BorderLayout.CENTER);
        dialog.add(btnPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    // --- экспорт ---

    private void exportTxt(String aiResult, String aiTitle, JDialog owner) {
        JFileChooser fc = new JFileChooser();
        if (fc.showSaveDialog(owner) != JFileChooser.APPROVE_OPTION) return;
        File file = ensureExt(fc.getSelectedFile(), ".txt");

        String sep = "-".repeat(60);
        String content = "MISSION ANALYZER — AI REPORT\n" + sep + "\n"
                + "Миссия:      " + mission.getMissionId() + "\n"
                + "Тип анализа: " + aiTitle + "\n"
                + sep + "\n\n"
                + "=== ОТЧЁТ ===\n"
                + reportText + "\n\n"
                + sep + "\n"
                + "=== АНАЛИЗ AI ===\n"
                + aiResult + "\n\n"
                + sep + "\n";

        try (FileWriter fw = new FileWriter(file)) {
            fw.write(content);
            JOptionPane.showMessageDialog(owner, "Сохранено:\n" + file.getAbsolutePath());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(owner, "Ошибка: " + e.getMessage());
        }
    }

    private void exportHtml(String aiResult, String aiTitle, JDialog owner) {
        JFileChooser fc = new JFileChooser();
        if (fc.showSaveDialog(owner) != JFileChooser.APPROVE_OPTION) return;
        File file = ensureExt(fc.getSelectedFile(), ".html");
        try {
            htmlExporter.exportAiResult(file, mission.getMissionId(),
                    aiTitle, reportText, aiResult);
            JOptionPane.showMessageDialog(owner, "Сохранено:\n" + file.getAbsolutePath());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(owner, "Ошибка: " + e.getMessage());
        }
    }

    private File ensureExt(File file, String ext) {
        return file.getName().toLowerCase().endsWith(ext)
                ? file : new File(file.getAbsolutePath() + ext);
    }
}