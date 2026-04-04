package ui;

import service.AppLogger;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.prefs.Preferences;

public class SettingsDialog extends JDialog {

    // настройки — статические чтобы сохранялись между открытиями
    private static final Preferences PREFS = Preferences.userRoot().node("mission_analyzer_settings");
    private static int fontSize = PREFS.getInt("fontSize", 15);
    private static boolean darkTheme = PREFS.getBoolean("darkTheme", false);

    private final JComboBox<String> fontSizeBox;
    private final JToggleButton themeToggle;
    private final Runnable onApply;

    // --- стили как в MainFrame ---
    private static final Color BORDER = new Color(210, 216, 224);
    private static final Color BG_SECONDARY = new Color(233, 239, 247);
    private static final Color TEXT = new Color(35, 42, 52);
    private static final Color BG_MAIN = new Color(245, 247, 250);

    private static final Font UI_FONT = new Font("Segoe UI", Font.PLAIN, 14);
    private static final Font UI_BOLD = new Font("Segoe UI", Font.BOLD, 14);

    public SettingsDialog(JFrame parent, Runnable onApply) {
        super(parent, "Настройки", true);
        this.onApply = onApply;

        fontSizeBox = new JComboBox<>(new String[]{
                "Маленький (12)",
                "Средний (15)",
                "Большой (18)",
                "Очень большой (22)"
        });

        themeToggle = new JToggleButton(darkTheme ? "Тёмная" : "Светлая");

        // восстанавливаем текущие значения
        fontSizeBox.setSelectedIndex(fontSizeToIndex(fontSize));
        themeToggle.setSelected(darkTheme);

        setSize(360, 260);
        setLocationRelativeTo(parent);
        setResizable(false);

        buildUI();
    }

    private void buildUI() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(BG_MAIN);
        panel.setBorder(new EmptyBorder(16, 20, 8, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 6, 8, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel fontLabel = new JLabel("Размер шрифта отчёта:");
        fontLabel.setFont(UI_BOLD);
        fontLabel.setForeground(TEXT);

        JLabel themeLabel = new JLabel("Тема оформления:");
        themeLabel.setFont(UI_BOLD);
        themeLabel.setForeground(TEXT);

        fontSizeBox.setFont(UI_FONT);

        themeToggle.setFont(UI_BOLD);
        themeToggle.setForeground(TEXT);
        themeToggle.setBackground(BG_SECONDARY);
        themeToggle.setFocusPainted(false);
        themeToggle.setBorder(new CompoundBorder(
                new LineBorder(BORDER, 1, true),
                new EmptyBorder(8, 16, 8, 16)
        ));
        themeToggle.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        themeToggle.addActionListener(e ->
                themeToggle.setText(themeToggle.isSelected() ? "Тёмная" : "Светлая")
        );

        // шрифт
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.4;
        panel.add(fontLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.6;
        panel.add(fontSizeBox, gbc);

        // тема
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.4;
        panel.add(themeLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.6;
        panel.add(themeToggle, gbc);

        // разделитель
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        panel.add(new JSeparator(), gbc);

        // экспорт логов
        gbc.gridy = 3;
        JButton exportLogsBtn = createPrimaryButton("Сохранить логи в файл");
        exportLogsBtn.addActionListener(e -> exportLogs());
        panel.add(exportLogsBtn, gbc);

        // кнопки снизу
        JButton applyBtn = createPrimaryButton("Применить");
        JButton cancelBtn = createSecondaryButton("Отмена");

        applyBtn.addActionListener(e -> applySettings());
        cancelBtn.addActionListener(e -> dispose());

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 8));
        btnPanel.setBackground(BG_MAIN);
        btnPanel.add(applyBtn);
        btnPanel.add(cancelBtn);

        setLayout(new BorderLayout());
        add(panel, BorderLayout.CENTER);
        add(btnPanel, BorderLayout.SOUTH);
    }

    private void applySettings() {
        fontSize = indexToFontSize(fontSizeBox.getSelectedIndex());
        darkTheme = themeToggle.isSelected();

        // сохраняем между перезапусками
        PREFS.putInt("fontSize", fontSize);
        PREFS.putBoolean("darkTheme", darkTheme);

        AppLogger.log("Настройки применены: шрифт=" + fontSize
                + ", тема=" + (darkTheme ? "тёмная" : "светлая"));

        onApply.run();
        dispose();
    }

    private void exportLogs() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Сохранить логи");
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File file = ensureExt(fc.getSelectedFile(), ".txt");

        try (FileWriter fw = new FileWriter(file)) {
            fw.write(AppLogger.getLogs());
            AppLogger.export("LOGS", file.getAbsolutePath());
            JOptionPane.showMessageDialog(this, "Логи сохранены:\n" + file.getAbsolutePath());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Ошибка: " + e.getMessage());
        }
    }

    public static int getFontSize() {
        return fontSize;
    }

    public static boolean isDarkTheme() {
        return darkTheme;
    }

    private int fontSizeToIndex(int size) {
        return switch (size) {
            case 12 -> 0;
            case 18 -> 2;
            case 22 -> 3;
            default -> 1;
        };
    }

    private int indexToFontSize(int index) {
        return switch (index) {
            case 0 -> 12;
            case 2 -> 18;
            case 3 -> 22;
            default -> 15;
        };
    }

    private File ensureExt(File file, String ext) {
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
        return button;
    }

    private JButton createSecondaryButton(String text) {
        JButton button = createPrimaryButton(text);
        button.setBackground(BG_SECONDARY);
        return button;
    }
}