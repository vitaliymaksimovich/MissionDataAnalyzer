package ui;

import service.filter.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class FilterDialog extends JDialog {

    private final FilterChain filterChain;
    private boolean applied = false;

    private final JComboBox<String> threatLevelBox;
    private final JComboBox<String> outcomeBox;
    private final JTextField dateFromField = new JTextField(10);
    private final JTextField dateToField   = new JTextField(10);

    private static final String ANY = "Любой";

    public FilterDialog(JFrame parent, FilterChain filterChain) {
        super(parent, "Фильтры миссий", true);
        this.filterChain = filterChain;

        threatLevelBox = new JComboBox<>(new String[]{
                ANY, "SPECIAL_GRADE", "HIGH", "MEDIUM", "LOW"
        });
        outcomeBox = new JComboBox<>(new String[]{
                ANY, "SUCCESS", "FAILURE", "PARTIAL_SUCCESS"
        });

        // восстанавливаем текущее состояние фильтров
        restoreState();

        setSize(360, 300);
        setLocationRelativeTo(parent);
        setResizable(false);
        buildUI();
    }

    private void restoreState() {
        for (MissionFilter f : filterChain.getFilters()) {
            if (f instanceof ThreatLevelFilter) {
                String val = f.getDisplayName().replace("Уровень угрозы: ", "");
                threatLevelBox.setSelectedItem(val);
            } else if (f instanceof OutcomeFilter) {
                String val = f.getDisplayName().replace("Результат: ", "");
                outcomeBox.setSelectedItem(val);
            } else if (f instanceof DateRangeFilter) {
                String val = f.getDisplayName().replace("Дата: ", "");
                String[] parts = val.split(" — ");
                if (parts.length == 2) {
                    dateFromField.setText(parts[0].trim());
                    dateToField.setText(parts[1].trim());
                }
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
        addRow(panel, gbc, 2, "Дата от:",        dateFromField);
        addRow(panel, gbc, 3, "Дата до:",        dateToField);

        JLabel hint = new JLabel("Формат даты: 2024-10-12");
        hint.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        hint.setForeground(Color.GRAY);
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
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

        String from = dateFromField.getText().trim();
        String to   = dateToField.getText().trim();
        if (!from.isBlank() || !to.isBlank())
            filterChain.add(new DateRangeFilter(from, to));

        applied = true;
        dispose();
    }

    private void resetFilters() {
        threatLevelBox.setSelectedIndex(0);
        outcomeBox.setSelectedIndex(0);
        dateFromField.setText("");
        dateToField.setText("");
        filterChain.clear();
        applied = true;
        dispose();
    }

    public boolean isApplied() { return applied; }
}