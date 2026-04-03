package ui;

import ai.AiReviewService;
import ai.AiServiceFactory;
import model.Mission;
import service.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
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
    private final List<File> loadedFiles       = new ArrayList<>();
    private Mission currentMission;
    private JButton activeButton;

    // --- сервисы ---
    private final MissionService missionService     = new MissionService();
    private final MissionBatchService batchService  = new MissionBatchService();
    private final BatchStatsFormatter statsFormatter = new BatchStatsFormatter();
    private final AiReviewService aiReviewService   = AiServiceFactory.create();
    private final HtmlReportExporter htmlExporter   = new HtmlReportExporter();

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

    public MainFrame() {
        reportTypeSelector = new JComboBox<>(formatters.toArray(new ReportFormatter[0]));
        reportTypeSelector.setRenderer((list, value, index, isSelected, cellHasFocus) -> {
            JLabel label = new JLabel(value != null ? value.getDisplayName() : "");
            if (isSelected) {
                label.setOpaque(true);
                label.setBackground(list.getSelectionBackground());
            }
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
                Image scaled = original.getImage().getScaledInstance(80, 80, Image.SCALE_DEFAULT);
                assistantGif = new ImageIcon(scaled);
            }
        } catch (Exception ignored) {}

        try {
            java.net.URL standUrl = getClass().getResource("/templates/assets/Assistant-stand.gif");
            if (standUrl != null) {
                ImageIcon original = new ImageIcon(standUrl);
                Image scaled = original.getImage().getScaledInstance(80, 80, Image.SCALE_DEFAULT);
                assistantStandGif = new ImageIcon(scaled);
            }
        } catch (Exception ignored) {}
    }

    private void setupWindow() {
        setTitle("Mission Analyzer");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(5, 5));

        add(buildTopPanel(), BorderLayout.NORTH);
        add(buildCenterPanel(), BorderLayout.CENTER);

        // gif-кнопка поверх всего через LayeredPane
        assistantBtn = buildAssistantButton();
        getLayeredPane().add(assistantBtn, JLayeredPane.POPUP_LAYER);

        // перепозиционируем кнопку при resize окна
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                repositionAssistantButton();
            }
        });
    }

    private void repositionAssistantButton() {
        Dimension size = getContentPane().getSize();
        assistantBtn.setBounds(size.width - 110, size.height - 95, 80, 80);
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

    private JButton buildAssistantButton() {
        JButton btn = assistantGif != null
                ? new JButton(assistantGif)
                : new JButton("AI");

        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setToolTipText("Спросить GigaChat");
        btn.setBounds(900, 580, 68, 68);

        btn.addActionListener(e -> {
            if (currentMission == null) {
                warn("Сначала откройте миссию для анализа.");
                return;
            }
            // передаём текущий текст отчёта
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

        // после перестройки списка перепозиционируем кнопку
        SwingUtilities.invokeLater(this::repositionAssistantButton);
    }

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
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        logArea.setText(service.AppLogger.getLogs());
        logArea.setCaretPosition(logArea.getDocument().getLength());
        JOptionPane.showMessageDialog(this, new JScrollPane(logArea), "Логи",
                JOptionPane.INFORMATION_MESSAGE);
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
        if (reportArea.getText().isBlank()) { warn("Сначала выполните анализ."); return; }
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
                ? file : new File(file.getAbsolutePath() + ext);
    }

    private void warn(String msg)  { JOptionPane.showMessageDialog(this, msg, "Предупреждение", JOptionPane.WARNING_MESSAGE); }
    private void error(String msg) { JOptionPane.showMessageDialog(this, msg, "Ошибка", JOptionPane.ERROR_MESSAGE); }
    private void info(String msg)  { JOptionPane.showMessageDialog(this, msg, "Успех", JOptionPane.INFORMATION_MESSAGE); }
}