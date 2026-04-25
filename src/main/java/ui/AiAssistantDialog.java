package ui;

import ai.AiReviewService;
import model.Mission;
import parser.plugin.ExportPlugin;
import service.HtmlReportExporter;
import service.PluginManager;

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
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Диалог AI-помощника.
 * Два режима: анализ одной миссии (mission != null) и общий анализ (mission == null, batch).
 */
public class AiAssistantDialog {

    private final JFrame parent;
    private final AiReviewService aiService;
    private final Mission mission;          // null → batch-режим
    private final List<Mission> allMissions; // для batch-анализа
    private final ImageIcon assistantIcon;
    private final HtmlReportExporter htmlExporter = new HtmlReportExporter();
    private final String reportText;

    private static final Font UI_FONT = new Font("Segoe UI", Font.PLAIN, 14);
    private static final Font UI_BOLD = new Font("Segoe UI", Font.BOLD, 14);
    private static final Font TITLE_FONT = new Font("Segoe UI", Font.BOLD, 15);
    private static final Font TEXT_FONT = new Font("Segoe UI", Font.PLAIN, 14);
    private static final Font LOADING_FONT = new Font("Segoe UI", Font.PLAIN, 13);

    public AiAssistantDialog(JFrame parent, AiReviewService aiService,
                             Mission mission, List<Mission> allMissions,
                             ImageIcon assistantIcon, String reportText) {
        this.parent = parent;
        this.aiService = aiService;
        this.mission = mission;
        this.allMissions = allMissions;
        this.assistantIcon = assistantIcon;
        this.reportText = reportText;
    }

    public void show() {
        showSelectionDialog();
    }

    private void showSelectionDialog() {
        boolean batchMode = (mission == null);
        String subtitle = batchMode
                ? "Общий анализ по " + allMissions.size() + " миссиям"
                : "Миссия: " + mission.getMissionId();

        JDialog dialog = createBaseDialog("Помощник GigaChat", 460, batchMode ? 360 : 520, true);
        dialog.setLayout(new BorderLayout());

        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBackground(AppTheme.bgMain());
        root.setBorder(new EmptyBorder(14, 14, 14, 14));

        // --- верхняя панель: картинка помощника + заголовок ---
        JPanel topPanel = new JPanel(new BorderLayout(8, 8));
        topPanel.setBackground(AppTheme.bgPanel());
        topPanel.setBorder(new CompoundBorder(
                new LineBorder(AppTheme.border(), 1, true),
                new EmptyBorder(12, 14, 10, 14)
        ));

        if (assistantIcon != null) {
            JLabel iconLabel = new JLabel(assistantIcon, SwingConstants.CENTER);
            iconLabel.setOpaque(false);
            topPanel.add(iconLabel, BorderLayout.CENTER);
        }

        JLabel titleLabel = new JLabel("Выберите действие", SwingConstants.CENTER);
        titleLabel.setFont(TITLE_FONT);
        titleLabel.setForeground(AppTheme.text());

        JLabel subtitleLabel = new JLabel(subtitle, SwingConstants.CENTER);
        subtitleLabel.setFont(UI_FONT);
        subtitleLabel.setForeground(AppTheme.textSecondary());

        JPanel textPanel = new JPanel();
        textPanel.setOpaque(false);
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        textPanel.add(titleLabel);
        textPanel.add(Box.createVerticalStrut(4));
        textPanel.add(subtitleLabel);

        topPanel.add(textPanel, BorderLayout.SOUTH);

        // --- центральная панель: кнопки действий ---
        JPanel centerPanel = new JPanel(new BorderLayout(0, 10));
        centerPanel.setBackground(AppTheme.bgMain());

        // кнопки быстрых действий
        Map<String, Supplier<String>> actions = new LinkedHashMap<>();
        if (batchMode) {
            actions.put("Общий анализ всех миссий", () -> aiService.analyzeBatch(allMissions));
        } else {
            actions.put("Краткий анализ", () -> aiService.generateBriefAnalysis(mission));
            actions.put("Подробный анализ", () -> aiService.generateDetailedAnalysis(mission));
            actions.put("Поиск проблем", () -> aiService.findProblems(mission));
            actions.put("Рекомендации", () -> aiService.generateRecommendations(mission));
            actions.put("История по миссии", () -> aiService.generateStory(mission));
        }

        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.Y_AXIS));
        buttonsPanel.setBackground(AppTheme.bgPanel());
        buttonsPanel.setBorder(new CompoundBorder(
                new LineBorder(AppTheme.border(), 1, true),
                new EmptyBorder(10, 10, 10, 10)
        ));

        for (Map.Entry<String, Supplier<String>> entry : actions.entrySet()) {
            JButton btn = createActionButton(entry.getKey());
            btn.addActionListener(e -> {
                dialog.dispose();
                showLoadingThenResult(entry.getKey(), entry.getValue());
            });
            buttonsPanel.add(btn);
            buttonsPanel.add(Box.createVerticalStrut(6));
        }
        if (buttonsPanel.getComponentCount() > 0)
            buttonsPanel.remove(buttonsPanel.getComponentCount() - 1);

        JScrollPane buttonsScroll = new JScrollPane(buttonsPanel);
        buttonsScroll.setBorder(new LineBorder(AppTheme.border(), 1, true));
        buttonsScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        buttonsScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        buttonsScroll.getVerticalScrollBar().setUnitIncrement(12);
        buttonsScroll.getViewport().setBackground(AppTheme.bgPanel());

        centerPanel.add(buttonsScroll, BorderLayout.CENTER);

        // --- поле «свой вопрос» (только для одиночной миссии) ---
        if (!batchMode) {
            JPanel questionPanel = new JPanel(new BorderLayout(6, 0));
            questionPanel.setBackground(AppTheme.bgPanel());
            questionPanel.setBorder(new CompoundBorder(
                    new LineBorder(AppTheme.border(), 1, true),
                    new EmptyBorder(10, 10, 10, 10)
            ));

            JLabel qLabel = new JLabel("Свой вопрос:");
            qLabel.setFont(UI_BOLD);
            qLabel.setForeground(AppTheme.text());

            JTextField questionField = new JTextField();
            questionField.setFont(UI_FONT);
            questionField.setToolTipText("Введите вопрос по миссии и нажмите Enter или кнопку");

            JButton askBtn = createPrimaryButton("Спросить");

            Runnable askAction = () -> {
                String q = questionField.getText().trim();
                if (q.isBlank()) return;
                dialog.dispose();
                showLoadingThenResult("Свой вопрос",
                        () -> aiService.customQuestion(mission, q));
            };

            askBtn.addActionListener(e -> askAction.run());
            questionField.addActionListener(e -> askAction.run());

            questionPanel.add(qLabel, BorderLayout.NORTH);

            JPanel inputRow = new JPanel(new BorderLayout(6, 0));
            inputRow.setOpaque(false);
            inputRow.setBorder(new EmptyBorder(6, 0, 0, 0));
            inputRow.add(questionField, BorderLayout.CENTER);
            inputRow.add(askBtn, BorderLayout.EAST);

            questionPanel.add(inputRow, BorderLayout.CENTER);
            centerPanel.add(questionPanel, BorderLayout.SOUTH);
        }

        // --- нижняя кнопка «Отмена» ---
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 4));
        bottomPanel.setBackground(AppTheme.bgMain());
        JButton cancelBtn = createSecondaryButton("Отмена");
        cancelBtn.addActionListener(e -> dialog.dispose());
        bottomPanel.add(cancelBtn);

        root.add(topPanel, BorderLayout.NORTH);
        root.add(centerPanel, BorderLayout.CENTER);
        root.add(bottomPanel, BorderLayout.SOUTH);

        dialog.add(root, BorderLayout.CENTER);
        dialog.setVisible(true);
    }

    private void showLoadingThenResult(String title, Supplier<String> action) {
        JDialog loadingDialog = createBaseDialog("GigaChat думает...", 360, 150, true);
        loadingDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        loadingDialog.setLayout(new BorderLayout());

        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBackground(AppTheme.bgMain());
        root.setBorder(new EmptyBorder(14, 14, 14, 14));

        JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
        contentPanel.setBackground(AppTheme.bgPanel());
        contentPanel.setBorder(new CompoundBorder(
                new LineBorder(AppTheme.border(), 1, true),
                new EmptyBorder(18, 18, 18, 18)
        ));

        JLabel label = new JLabel("Запрос отправлен, ожидаем ответ...", SwingConstants.CENTER);
        label.setFont(LOADING_FONT);
        label.setForeground(AppTheme.text());

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
                return action.get();
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
        // Заголовок окна — короткий, без длинных текстов вопросов
        String windowTitle = "GigaChat — " + title;
        if (windowTitle.length() > 50) windowTitle = "GigaChat — " + title.substring(0, 40) + "...";
        JDialog dialog = createBaseDialog(windowTitle, 680, 520, true);
        dialog.setLayout(new BorderLayout());

        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBackground(AppTheme.bgMain());
        root.setBorder(new EmptyBorder(14, 14, 14, 14));

        JLabel headerLabel = new JLabel(title);
        headerLabel.setFont(TITLE_FONT);
        headerLabel.setForeground(AppTheme.text());

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(AppTheme.bgMain());
        headerPanel.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 1, 0, AppTheme.border()),
                new EmptyBorder(0, 2, 8, 2)
        ));
        headerPanel.add(headerLabel, BorderLayout.WEST);

        JTextArea resultArea = new JTextArea(result);
        resultArea.setEditable(false);
        resultArea.setLineWrap(true);
        resultArea.setWrapStyleWord(true);
        resultArea.setFont(TEXT_FONT);
        resultArea.setForeground(AppTheme.text());
        resultArea.setBackground(AppTheme.bgPanel());
        resultArea.setMargin(new Insets(14, 14, 14, 14));
        resultArea.setBorder(null);
        resultArea.setCaretPosition(0);

        JScrollPane scroll = new JScrollPane(resultArea);
        styleScrollPane(scroll);
        scroll.setBorder(createTitledBorder(title));

        JButton closeBtn  = createSecondaryButton("Закрыть");
        JButton copyBtn   = createSecondaryButton("Скопировать");
        JButton exportBtn = createPrimaryButton("Экспорт ▼");

        closeBtn.addActionListener(e -> dialog.dispose());

        copyBtn.addActionListener(e -> {
            Toolkit.getDefaultToolkit()
                    .getSystemClipboard()
                    .setContents(new StringSelection(result), null);
            JOptionPane.showMessageDialog(
                    dialog, "Скопировано!", "Успех", JOptionPane.INFORMATION_MESSAGE);
        });

        // Выпадающее меню экспорта
        exportBtn.addActionListener(e -> showExportMenu(exportBtn, result, title, dialog));

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        btnPanel.setBackground(AppTheme.bgMain());
        btnPanel.add(closeBtn);
        btnPanel.add(copyBtn);
        btnPanel.add(exportBtn);

        root.add(headerPanel, BorderLayout.NORTH);
        root.add(scroll, BorderLayout.CENTER);
        root.add(btnPanel, BorderLayout.SOUTH);

        dialog.add(root, BorderLayout.CENTER);
        dialog.setVisible(true);
    }

    // --- экспорт ---

    /**
     * Выпадающее меню экспорта: встроенные форматы + плагины.
     * Появляется под кнопкой «Экспорт ▼» в диалоге результата.
     */
    private void showExportMenu(Component anchor, String aiResult, String aiTitle, JDialog owner) {
        JPopupMenu menu = new JPopupMenu();

        // Встроенные форматы
        JMenuItem txtItem = new JMenuItem("TXT (отчёт + AI)");
        txtItem.addActionListener(e -> exportTxt(aiResult, aiTitle, owner));

        JMenuItem htmlItem = new JMenuItem("HTML (отчёт + AI)");
        htmlItem.addActionListener(e -> exportHtml(aiResult, aiTitle, owner));

        menu.add(txtItem);
        menu.add(htmlItem);

        // Плагины экспорта из PluginManager
        java.util.List<ExportPlugin> plugins = PluginManager.getInstance().getActiveExportPlugins();
        if (!plugins.isEmpty()) {
            menu.addSeparator();
            for (ExportPlugin plugin : plugins) {
                JMenuItem item = new JMenuItem(plugin.getMetadata().name());
                item.addActionListener(ev -> exportWithPlugin(plugin, aiResult, aiTitle, owner));
                menu.add(item);
            }
        }

        menu.show(anchor, 0, anchor.getHeight());
    }

    /** Экспорт через плагин — объединяет текст отчёта и AI-анализа */
    private void exportWithPlugin(ExportPlugin plugin, String aiResult, String aiTitle, JDialog owner) {
        JFileChooser fc = new JFileChooser();
        if (fc.showSaveDialog(owner) != JFileChooser.APPROVE_OPTION) return;
        File file = ensureExt(fc.getSelectedFile(), plugin.getFileExtension());

        // Объединяем отчёт и AI-анализ в один текст
        String combined = "=== ОТЧЁТ ===\n" + reportText
                + "\n\n=== AI АНАЛИЗ: " + aiTitle + " ===\n" + aiResult;
        try {
            plugin.export(combined, file);
            JOptionPane.showMessageDialog(owner, "Сохранено:\n" + file.getAbsolutePath());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(owner, "Ошибка: " + e.getMessage());
        }
    }

    private void exportTxt(String aiResult, String aiTitle, JDialog owner) {
        JFileChooser fc = new JFileChooser();
        if (fc.showSaveDialog(owner) != JFileChooser.APPROVE_OPTION) return;
        File file = ensureExt(fc.getSelectedFile(), ".txt");

        String sep = "-".repeat(60);
        String header = mission != null
                ? "Миссия:      " + mission.getMissionId()
                : "Общий анализ по " + allMissions.size() + " миссиям";
        String content = "MISSION ANALYZER — AI REPORT\n" + sep + "\n"
                + header + "\n"
                + "Тип анализа: " + aiTitle + "\n"
                + sep + "\n\n"
                + "=== ОТЧЁТ ===\n"
                + reportText + "\n\n"
                + sep + "\n"
                + "=== АНАЛИЗ AI ===\n"
                + aiResult + "\n\n"
                + sep + "\n";

        try (FileWriter fw = new FileWriter(file, StandardCharsets.UTF_8)) {
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
        String missionLabel = mission != null ? mission.getMissionId() : "Все миссии";
        try {
            htmlExporter.exportAiResult(file, missionLabel, aiTitle, reportText, aiResult);
            JOptionPane.showMessageDialog(owner, "Сохранено:\n" + file.getAbsolutePath());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(owner, "Ошибка: " + e.getMessage());
        }
    }

    // --- утилиты ---

    private File ensureExt(File file, String ext) {
        return file.getName().toLowerCase().endsWith(ext)
                ? file : new File(file.getAbsolutePath() + ext);
    }

    private JDialog createBaseDialog(String title, int width, int height, boolean modal) {
        JDialog dialog = new JDialog(parent, title, modal);
        dialog.setSize(width, height);
        dialog.setLocationRelativeTo(parent);
        dialog.setResizable(false);
        dialog.getContentPane().setBackground(AppTheme.bgMain());
        return dialog;
    }

    private JButton createActionButton(String text) {
        JButton button = new JButton(text);
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        button.setFont(UI_BOLD);
        button.setForeground(AppTheme.text());
        button.setBackground(AppTheme.bgSecondary());
        button.setFocusPainted(false);
        button.setBorder(new CompoundBorder(
                new LineBorder(AppTheme.border(), 1, true),
                new EmptyBorder(8, 16, 8, 16)
        ));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    private JButton createPrimaryButton(String text) {
        JButton button = new JButton(text);
        button.setFont(UI_BOLD);
        button.setForeground(AppTheme.text());
        button.setBackground(AppTheme.bgPanel());
        button.setFocusPainted(false);
        button.setBorder(new CompoundBorder(
                new LineBorder(AppTheme.border(), 1, true),
                new EmptyBorder(8, 16, 8, 16)
        ));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(button.getPreferredSize().width, 38));
        return button;
    }

    private JButton createSecondaryButton(String text) {
        JButton button = createPrimaryButton(text);
        button.setBackground(AppTheme.bgSecondary());
        return button;
    }

    private void styleScrollPane(JScrollPane scrollPane) {
        scrollPane.getViewport().setBackground(AppTheme.bgPanel());
        scrollPane.setBackground(AppTheme.bgPanel());
        scrollPane.setBorder(new LineBorder(AppTheme.border(), 1, true));
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
}
