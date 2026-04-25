package ui;

import model.Mission;
import service.filter.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Set;

public class FilterDialog extends JDialog {

    private final Set<Mission> favorites;
    private final FilterChain filterChain;
    private boolean applied = false;

    private final JComboBox<String> threatLevelBox;
    private final JComboBox<String> outcomeBox;
    private final JTextField sorcererField = new JTextField(10);
    private final JTextField dateFromField = new JTextField(10);
    private final JTextField dateToField   = new JTextField(10);
    private final JCheckBox favoritesOnlyBox = new JCheckBox("Только избранные");

    private static final String ANY = "Любой";

    public FilterDialog(JFrame parent, FilterChain filterChain, Set<Mission> favorites) {
        super(parent, "Фильтры миссий", true);
        this.favorites = favorites;
        this.filterChain = filterChain;

        threatLevelBox = new JComboBox<>(new String[]{
                ANY, "SPECIAL_GRADE", "HIGH", "MEDIUM", "LOW"
        });
        outcomeBox = new JComboBox<>(new String[]{
                ANY, "SUCCESS", "FAILURE", "PARTIAL_SUCCESS"
        });

        // восстанавливаем текущее состояние фильтров
        restoreState();

        setSize(360, 380);
        setLocationRelativeTo(parent);
        setResizable(false);
        buildUI();
    }

    private void restoreState() {
        for (MissionFilter f : filterChain.getFilters()) {
            switch (f.getFilterId()) {
                case "threatLevel" -> threatLevelBox.setSelectedItem(f.getValue());
                case "outcome"     -> outcomeBox.setSelectedItem(f.getValue());
                case "sorcerer"    -> sorcererField.setText(f.getValue());
                case "dateRange"   -> {
                    String[] parts = f.getValue().split("\\|", 2);
                    if (parts.length == 2) {
                        dateFromField.setText(parts[0]);
                        dateToField.setText(parts[1]);
                    }
                }
                case "favorites"   -> favoritesOnlyBox.setSelected(true);
            }
        }
    }

    private void buildUI() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(16, 20, 8, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        addRow(panel, gbc, 0, "Уровень угрозы:", threatLevelBox);
        addRow(panel, gbc, 1, "Результат:",      outcomeBox);
        addRow(panel, gbc, 2, "Участник:",        sorcererField);
        addRow(panel, gbc, 3, "Дата от:",         dateFromField);
        addRow(panel, gbc, 4, "Дата до:",         dateToField);

        // Чекбокс «Только избранные» — занимает обе колонки
        favoritesOnlyBox.setOpaque(false);
        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 2; gbc.weightx = 1.0;
        panel.add(favoritesOnlyBox, gbc);

        JLabel hint = new JLabel("Формат даты: 2024-10-12");
        hint.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        hint.setForeground(AppTheme.textSecondary());
        gbc.gridx = 0; gbc.gridy = 6; gbc.gridwidth = 2;
        panel.add(hint, gbc);

        JButton applyBtn  = new JButton("Применить");
        JButton resetBtn  = new JButton("Сбросить");
        JButton cancelBtn = new JButton("Отмена");

        applyBtn.addActionListener(e -> applyFilters());
        resetBtn.addActionListener(e -> resetFilters());
        cancelBtn.addActionListener(e -> dispose());

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 8));
        btnPanel.add(applyBtn);
        btnPanel.add(resetBtn);
        btnPanel.add(cancelBtn);

        setLayout(new BorderLayout());
        add(panel, BorderLayout.CENTER);
        add(btnPanel, BorderLayout.SOUTH);
    }

    private void addRow(JPanel panel, GridBagConstraints gbc,
                        int row, String label, JComponent field) {
        gbc.gridwidth = 1;
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.3;
        panel.add(new JLabel(label), gbc);
        gbc.gridx = 1; gbc.weightx = 0.7;
        panel.add(field, gbc);
    }

    private void applyFilters() {
        filterChain.clear();

        String threat = (String) threatLevelBox.getSelectedItem();
        if (threat != null && !threat.equals(ANY))
            filterChain.add(new ThreatLevelFilter(threat));

        String outcome = (String) outcomeBox.getSelectedItem();
        if (outcome != null && !outcome.equals(ANY))
            filterChain.add(new OutcomeFilter(outcome));

        String sorcerer = sorcererField.getText().trim();
        if (!sorcerer.isBlank())
            filterChain.add(new SorcererFilter(sorcerer));

        String from = dateFromField.getText().trim();
        String to   = dateToField.getText().trim();
        if (!from.isBlank() || !to.isBlank())
            filterChain.add(new DateRangeFilter(from, to));

        if (favoritesOnlyBox.isSelected())
            filterChain.add(new FavoritesFilter(favorites));

        applied = true;
        dispose();
    }

    private void resetFilters() {
        threatLevelBox.setSelectedIndex(0);
        outcomeBox.setSelectedIndex(0);
        sorcererField.setText("");
        dateFromField.setText("");
        dateToField.setText("");
        favoritesOnlyBox.setSelected(false);
        filterChain.clear();
        applied = true;
        dispose();
    }

    public boolean isApplied() { return applied; }
}
