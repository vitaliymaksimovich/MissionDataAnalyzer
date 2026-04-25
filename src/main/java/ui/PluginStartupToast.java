package ui;

import parser.MissionParser;
import parser.plugin.ArchivePlugin;
import parser.plugin.ExportPlugin;
import parser.plugin.ReportFormatterPlugin;
import service.PluginManager;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.List;

/**
 * Всплывающее уведомление о загруженных плагинах — появляется при старте приложения.
 * Неблокирующий (не модальный), автоматически закрывается через AUTO_CLOSE_MS мс.
 * Показывается только если загружен хотя бы один плагин или произошла ошибка.
 */
public class PluginStartupToast extends JDialog {

    private static final int AUTO_CLOSE_MS  = 6000;  // время жизни в мс
    private static final int TICK_MS        = 1000;   // интервал таймера
    private static final Font UI_FONT       = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font UI_BOLD       = new Font("Segoe UI", Font.BOLD,  13);
    private static final Font SMALL_FONT    = new Font("Segoe UI", Font.PLAIN, 11);

    private int secondsLeft;
    private JLabel countdownLabel;

    // -------------------------------------------------------------------------
    // Фабричный метод — точка входа
    // -------------------------------------------------------------------------

    /**
     * Загружает плагины (если ещё не загружены) и показывает тост, если есть что показать.
     * Вызывать ПОСЛЕ frame.setVisible(true), чтобы правильно позиционировать.
     */
    public static void showIfNeeded(JFrame parent) {
        PluginManager pm = PluginManager.getInstance();
        pm.loadIfNeeded();
        PluginManager.LoadResult result = pm.getLastResult();

        if (result == null) return;

        int total = result.parsersFound() + result.exportsFound()
                  + result.reportsFound();

        // Показываем если есть плагины или была ошибка загрузки
        boolean hasPlugins = total > 0;
        boolean hasError   = result.error() != null;

        if (!hasPlugins && !hasError) return;

        PluginStartupToast toast = new PluginStartupToast(parent, pm, result);
        toast.setVisible(true);
    }

    // -------------------------------------------------------------------------
    // Конструктор
    // -------------------------------------------------------------------------

    private PluginStartupToast(JFrame parent, PluginManager pm, PluginManager.LoadResult result) {
        super(parent, false); // false = не модальный
        setUndecorated(true);
        setFocusableWindowState(false); // не перехватываем фокус у главного окна

        buildUI(pm, result);
        pack();
        positionBottomRight(parent);

        // Таймер автозакрытия
        secondsLeft = AUTO_CLOSE_MS / TICK_MS;
        updateCountdown();

        Timer timer = new Timer(TICK_MS, null);
        timer.addActionListener(e -> {
            secondsLeft--;
            updateCountdown();
            if (secondsLeft <= 0) {
                timer.stop();
                dispose();
            }
        });
        timer.start();
    }

    // -------------------------------------------------------------------------
    // Построение UI
    // -------------------------------------------------------------------------

    private void buildUI(PluginManager pm, PluginManager.LoadResult result) {
        JPanel root = new JPanel(new BorderLayout(0, 8));
        root.setBackground(AppTheme.bgPanel());
        root.setBorder(new CompoundBorder(
                new LineBorder(AppTheme.border(), 1, true),
                new EmptyBorder(12, 16, 10, 16)
        ));

        // --- Заголовок ---
        root.add(buildHeader(result), BorderLayout.NORTH);

        // --- Список плагинов ---
        int total = result.parsersFound() + result.exportsFound()
                  + result.reportsFound();
        if (total > 0) {
            root.add(buildPluginList(pm), BorderLayout.CENTER);
        }

        // --- Ошибка ---
        if (result.error() != null) {
            JLabel errLabel = new JLabel("⚠ " + result.error());
            errLabel.setFont(SMALL_FONT);
            errLabel.setForeground(new Color(210, 80, 80));
            errLabel.setBorder(new EmptyBorder(4, 0, 0, 0));
            root.add(errLabel, BorderLayout.SOUTH);
        }

        // --- Нижняя панель: кнопка + таймер ---
        root.add(buildFooter(), BorderLayout.SOUTH);

        setContentPane(root);
    }

    private JPanel buildHeader(PluginManager.LoadResult result) {
        int total = result.parsersFound() + result.exportsFound()
                  + result.reportsFound();

        String title = total > 0
                ? "Плагины загружены (" + total + ")"
                : "Плагины";

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(UI_BOLD);
        titleLabel.setForeground(AppTheme.text());

        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.add(titleLabel, BorderLayout.WEST);
        return panel;
    }

    private JPanel buildPluginList(PluginManager pm) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(6, 0, 4, 0));

        // Парсеры форматов + архивные распаковщики (для пользователя — одна категория)
        for (MissionParser p : pm.getAllParserPlugins()) {
            panel.add(buildRow(p.getMetadata().name(), "Парсер", new Color(80, 160, 220)));
        }
        for (ArchivePlugin ap : pm.getAllArchivePlugins()) {
            panel.add(buildRow(ap.getMetadata().name(), "Парсер", new Color(80, 160, 220)));
        }
        // Экспорт
        for (ExportPlugin ep : pm.getAllExportPlugins()) {
            panel.add(buildRow(ep.getMetadata().name(), "Экспорт", new Color(100, 190, 120)));
        }
        // Отчёты
        for (ReportFormatterPlugin rp : pm.getAllReportPlugins()) {
            panel.add(buildRow(rp.getMetadata().name(), "Отчёт", new Color(200, 140, 80)));
        }

        return panel;
    }

    /** Одна строка: цветная метка типа + название плагина */
    private JPanel buildRow(String name, String typeName, Color typeColor) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        row.setOpaque(false);

        // Цветная бирка типа
        JLabel typeLabel = new JLabel(typeName);
        typeLabel.setFont(SMALL_FONT);
        typeLabel.setForeground(Color.WHITE);
        typeLabel.setBackground(typeColor);
        typeLabel.setOpaque(true);
        typeLabel.setBorder(new EmptyBorder(1, 5, 1, 5));

        // Название плагина
        JLabel nameLabel = new JLabel(name);
        nameLabel.setFont(UI_FONT);
        nameLabel.setForeground(AppTheme.text());

        row.add(typeLabel);
        row.add(nameLabel);
        return row;
    }

    private JPanel buildFooter() {
        countdownLabel = new JLabel();
        countdownLabel.setFont(SMALL_FONT);
        countdownLabel.setForeground(AppTheme.textSecondary());

        JButton closeBtn = new JButton("OK");
        closeBtn.setFont(UI_FONT);
        closeBtn.setFocusPainted(false);
        closeBtn.addActionListener(e -> dispose());

        JPanel panel = new JPanel(new BorderLayout(8, 0));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(6, 0, 0, 0));
        panel.add(countdownLabel, BorderLayout.WEST);
        panel.add(closeBtn,       BorderLayout.EAST);
        return panel;
    }

    // -------------------------------------------------------------------------
    // Вспомогательные методы
    // -------------------------------------------------------------------------

    private void updateCountdown() {
        if (countdownLabel != null)
            countdownLabel.setText("Закроется через " + secondsLeft + " с");
    }

    /** Позиционирует диалог в правый нижний угол окна приложения с отступом */
    private void positionBottomRight(JFrame parent) {
        int margin = 12;
        int x = parent.getX() + parent.getWidth()  - getWidth()  - margin;
        int y = parent.getY() + parent.getHeight() - getHeight() - margin;
        setLocation(x, y);
    }
}
