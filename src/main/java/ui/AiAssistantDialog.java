package ui;

import ai.AiReviewService;
import model.Mission;
import service.HtmlReportExporter;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
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

    // --- стили ---
    private static final Color BG_MAIN = new Color(245, 247, 250);
    private static final Color BG_PANEL = Color.WHITE;
    private static final Color BG_SECONDARY = new Color(233, 239, 247);
    private static final Color BORDER = new Color(210, 216, 224);
    private static final Color TEXT = new Color(35, 42, 52);

    private static final Font UI_FONT = new Font("Segoe UI", Font.PLAIN, 14);
    private static final Font UI_BOLD = new Font("Segoe UI", Font.BOLD, 14);
    private static final Font TITLE_FONT = new Font("Segoe UI", Font.BOLD, 15);
    private static final Font TEXT_FONT = new Font("Segoe UI", Font.PLAIN, 14);
    private static final Font LOADING_FONT = new Font("Segoe UI", Font.PLAIN, 13);

    public AiAssistantDialog(JFrame parent, AiReviewService aiService,
                             Mission mission, ImageIcon gifIcon, String reportText) {
        this.parent = parent;
        this.aiService = aiService;
        this.mission = mission;
        this.gifIcon = gifIcon;
        this.reportText = reportText;

        options.put("Краткий анализ", aiService::generateBriefAnalysis);
        options.put("Подробный анализ", aiService::generateDetailedAnalysis);
        options.put("Поиск проблем", aiService::findProblems);
        options.put("Рекомендации", aiService::generateRecommendations);
        options.put("История по мотивам", aiService::generateStory);
    }

    public void show() {
        showSelectionDialog();
    }

    private void showSelectionDialog() {
        JDialog dialog = createBaseDialog("Помощник GigaChat", 400, 470, true);
        dialog.setLayout(new BorderLayout(12, 12));

        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBackground(BG_MAIN);
        root.setBorder(new EmptyBorder(14, 14, 14, 14));

        JPanel topPanel = new JPanel(new BorderLayout(8, 10));
        topPanel.setBackground(BG_PANEL);
        topPanel.setBorder(new CompoundBorder(
                new LineBorder(BORDER, 1, true),
                new EmptyBorder(14, 14, 12, 14)
        ));

        if (gifIcon != null) {
            JLabel gifLabel = new JLabel(gifIcon, SwingConstants.CENTER);
            gifLabel.setOpaque(false);
            topPanel.add(gifLabel, BorderLayout.CENTER);
        }

        JLabel titleLabel = new JLabel("Выберите тип анализа", SwingConstants.CENTER);
        titleLabel.setFont(TITLE_FONT);
        titleLabel.setForeground(TEXT);

        JLabel questionLabel = new JLabel("Что вас интересует по текущей миссии?", SwingConstants.CENTER);
        questionLabel.setFont(UI_FONT);
        questionLabel.setForeground(TEXT);

        JPanel textPanel = new JPanel();
        textPanel.setOpaque(false);
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        questionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        textPanel.add(titleLabel);
        textPanel.add(Box.createVerticalStrut(6));
        textPanel.add(questionLabel);

        topPanel.add(textPanel, BorderLayout.SOUTH);

        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.Y_AXIS));
        buttonsPanel.setBackground(BG_PANEL);
        buttonsPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        for (Map.Entry<String, Function<Mission, String>> entry : options.entrySet()) {
            JButton btn = createPrimaryButton(entry.getKey());
            btn.setAlignmentX(Component.CENTER_ALIGNMENT);
            btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
            btn.addActionListener(e -> {
                dialog.dispose();
                showLoadingThenResult(entry.getKey(), entry.getValue());
            });
            buttonsPanel.add(btn);
            buttonsPanel.add(Box.createVerticalStrut(8));
        }

        if (buttonsPanel.getComponentCount() > 0) {
            buttonsPanel.remove(buttonsPanel.getComponentCount() - 1);
        }

        JPanel scrollWrapper = new JPanel(new BorderLayout());
        scrollWrapper.setBackground(BG_PANEL);
        scrollWrapper.setBorder(new CompoundBorder(
                new LineBorder(BORDER, 1, true),
                new EmptyBorder(6, 6, 6, 6)
        ));
        scrollWrapper.setPreferredSize(new Dimension(320, 210));

        JScrollPane scroll = new JScrollPane(buttonsPanel);
        scroll.setBorder(null);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll.getVerticalScrollBar().setUnitIncrement(12);
        scroll.getViewport().setBackground(BG_PANEL);

        scrollWrapper.add(scroll, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        bottomPanel.setBackground(BG_MAIN);

        JButton cancelBtn = createSecondaryButton("Отмена");
        cancelBtn.addActionListener(e -> dialog.dispose());
        bottomPanel.add(cancelBtn);

        root.add(topPanel, BorderLayout.NORTH);
        root.add(scrollWrapper, BorderLayout.CENTER);
        root.add(bottomPanel, BorderLayout.SOUTH);

        dialog.add(root, BorderLayout.CENTER);
        dialog.setVisible(true);
    }

    private void showLoadingThenResult(String title, Function<Mission, String> action) {
        JDialog loadingDialog = createBaseDialog("GigaChat думает...", 360, 150, true);
        loadingDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        loadingDialog.setLayout(new BorderLayout());

        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBackground(BG_MAIN);
        root.setBorder(new EmptyBorder(14, 14, 14, 14));

        JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
        contentPanel.setBackground(BG_PANEL);
        contentPanel.setBorder(new CompoundBorder(
                new LineBorder(BORDER, 1, true),
                new EmptyBorder(18, 18, 18, 18)
        ));

        JLabel label = new JLabel("Запрос отправлен, ожидаем ответ...", SwingConstants.CENTER);
        label.setFont(LOADING_FONT);
        label.setForeground(TEXT);

        JProgressBar progress = new JProgressBar();
        progress.setIndeterminate(true);
        progress.setBorder(new EmptyBorder(4, 6, 0, 6));

        contentPanel.add(label, BorderLayout.CENTER);
        contentPanel.add(progress, BorderLayout.SOUTH);
        root.add(contentPanel, BorderLayout.CENTER);

        loadingDialog.add(root, BorderLayout.CENTER);

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

    private void showResultDialog(String title, String result) {
        JDialog dialog = createBaseDialog("GigaChat — " + title, 680, 520, true);
        dialog.setLayout(new BorderLayout());

        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBackground(BG_MAIN);
        root.setBorder(new EmptyBorder(14, 14, 14, 14));

        JLabel headerLabel = new JLabel(title);
        headerLabel.setFont(TITLE_FONT);
        headerLabel.setForeground(TEXT);

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(BG_MAIN);
        headerPanel.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 1, 0, BORDER),
                new EmptyBorder(0, 2, 8, 2)
        ));
        headerPanel.add(headerLabel, BorderLayout.WEST);

        JTextArea resultArea = new JTextArea(result);
        resultArea.setEditable(false);
        resultArea.setLineWrap(true);
        resultArea.setWrapStyleWord(true);
        resultArea.setFont(TEXT_FONT);
        resultArea.setForeground(TEXT);
        resultArea.setBackground(BG_PANEL);
        resultArea.setMargin(new Insets(14, 14, 14, 14));
        resultArea.setBorder(null);
        resultArea.setCaretPosition(0);

        JScrollPane scroll = new JScrollPane(resultArea);
        styleScrollPane(scroll);
        scroll.setBorder(createTitledBorder(title));

        JButton closeBtn = createSecondaryButton("Закрыть");
        JButton copyBtn = createSecondaryButton("Скопировать");
        JButton txtBtn = createPrimaryButton("Экспорт TXT + AI");
        JButton htmlBtn = createPrimaryButton("Экспорт HTML + AI");

        closeBtn.addActionListener(e -> dialog.dispose());

        copyBtn.addActionListener(e -> {
            Toolkit.getDefaultToolkit()
                    .getSystemClipboard()
                    .setContents(new StringSelection(result), null);
            JOptionPane.showMessageDialog(
                    dialog,
                    "Скопировано!",
                    "Успех",
                    JOptionPane.INFORMATION_MESSAGE
            );
        });

        txtBtn.addActionListener(e -> exportTxt(result, title, dialog));
        htmlBtn.addActionListener(e -> exportHtml(result, title, dialog));

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        btnPanel.setBackground(BG_MAIN);
        btnPanel.add(closeBtn);
        btnPanel.add(copyBtn);
        btnPanel.add(txtBtn);
        btnPanel.add(htmlBtn);

        root.add(headerPanel, BorderLayout.NORTH);
        root.add(scroll, BorderLayout.CENTER);
        root.add(btnPanel, BorderLayout.SOUTH);

        dialog.add(root, BorderLayout.CENTER);
        dialog.setVisible(true);
    }

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
            htmlExporter.exportAiResult(file, mission.getMissionId(), aiTitle, reportText, aiResult);
            JOptionPane.showMessageDialog(owner, "Сохранено:\n" + file.getAbsolutePath());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(owner, "Ошибка: " + e.getMessage());
        }
    }

    private File ensureExt(File file, String ext) {
        return file.getName().toLowerCase().endsWith(ext)
                ? file
                : new File(file.getAbsolutePath() + ext);
    }

    private JDialog createBaseDialog(String title, int width, int height, boolean modal) {
        JDialog dialog = new JDialog(parent, title, modal);
        dialog.setSize(width, height);
        dialog.setLocationRelativeTo(parent);
        dialog.setResizable(false);
        dialog.getContentPane().setBackground(BG_MAIN);
        return dialog;
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
}