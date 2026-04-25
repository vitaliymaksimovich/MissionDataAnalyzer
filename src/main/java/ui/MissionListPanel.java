package ui;

import ai.AiReviewService;
import model.Mission;
import service.filter.FilterChain;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Левая панель приложения: список миссий + поиск + фильтры + сортировка + статистика + помощник.
 * SRP: отвечает только за отображение и управление списком миссий.
 * Общается с MainFrame через интерфейс Host — нет прямой зависимости на конкретный класс.
 */
public class MissionListPanel extends JPanel {

    /**
     * Паттерн: Observer (вариант Callback / Host interface).
     *
     * MissionListPanel не зависит напрямую от MainFrame — только от этого интерфейса.
     * Когда пользователь выбирает миссию, нажимает «Статистика» или «Очистить»,
     * панель вызывает соответствующий метод хоста, не зная, кто его реализует.
     *
     * MainFrame реализует Host и реагирует на события (обновляет отчёт, очищает данные).
     * Это обеспечивает слабую связанность (SRP + DIP): панель не знает о главном окне.
     */
    public interface Host {
        JFrame       getFrame();
        List<Mission> getMissions();
        Set<Mission>  getFavorites();
        FilterChain   getFilterChain();
        Mission       getCurrentMission();
        String        getReportText();
        AiReviewService getAiService();
        ImageIcon     getAssistantSleepIcon();
        void onMissionSelected(int index);  // пользователь выбрал миссию из списка
        void onShowStats();                 // нажата кнопка «Общая статистика»
        void onClearConfirmed();            // пользователь подтвердил очистку
    }

    // --- шрифты ---
    private static final Font UI_FONT    = new Font("Segoe UI", Font.PLAIN, 14);
    private static final Font UI_BOLD    = new Font("Segoe UI", Font.BOLD,  14);
    private static final Font TITLE_FONT = new Font("Segoe UI", Font.BOLD,  15);

    // --- текстовые константы (выносим, чтобы не дублировать в 4 местах) ---
    private static final String SEARCH_PLACEHOLDER = "ID, локация, результат, проклятие";

    private final Host host;

    // --- состояние сортировки ---
    private Comparator<Mission> currentSort     = null;
    private String              currentSortName = null;

    // --- внутренние компоненты ---
    private final JPanel  buttonPanel = new JPanel();  // контейнер кнопок миссий
    // карта Mission → кнопка (IdentityHashMap: сравнение по ссылке, не по equals)
    // используется вместо поиска по тексту — надёжнее при null/одинаковых ID
    private final Map<Mission, JButton> missionButtons = new IdentityHashMap<>();
    private JScrollPane   missionScroll;               // скролл-панель списка миссий
    private JPanel        bottomPanel;                 // нижний блок (bgMain)
    private JPanel        searchPanel;                 // панель поиска (bgPanel)
    private JPanel        actionsPanel;                // панель действий (bgPanel)
    private JButton       filterBtn;
    private JTextField    searchField;
    private JLabel        counterLabel;
    private JButton       sortBtn;
    private JButton       clearBtn;
    private JButton       activeButton;   // кнопка текущей выбранной миссии

    public MissionListPanel(Host host) {
        this.host = host;
        setLayout(new BorderLayout(0, 8));
        setBackground(AppTheme.bgMain());
        setBorder(new EmptyBorder(8, 8, 8, 0));
        setPreferredSize(new Dimension(280, 0));
        buildUI();
    }

    // -------------------------------------------------------------------------
    // Публичный API
    // -------------------------------------------------------------------------

    /** Перестраивает список с учётом текущих поиска/фильтра/сортировки */
    public void rebuild() {
        applySearchAndFilter();
    }

    /**
     * Выделяет кнопку указанной миссии.
     * Вызывается из MainFrame после программной смены currentMission.
     */
    public void setSelectedMission(Mission mission) {
        if (mission == null) return;
        JButton btn = missionButtons.get(mission);
        if (btn != null) setActiveButton(btn);
    }

    /** Обновляет цвета всех внутренних компонентов при смене темы */
    public void onThemeChanged() {
        // Фон самой панели
        setBackground(AppTheme.bgMain());

        // Нижний блок
        if (bottomPanel != null) bottomPanel.setBackground(AppTheme.bgMain());

        // Список миссий + скролл
        buttonPanel.setBackground(AppTheme.bgPanel());
        if (missionScroll != null) {
            missionScroll.setBackground(AppTheme.bgPanel());
            missionScroll.getViewport().setBackground(AppTheme.bgPanel());
            updateBorderColors(missionScroll);
        }

        // Панель поиска
        if (searchPanel != null) {
            searchPanel.setBackground(AppTheme.bgPanel());
            updateBorderColors(searchPanel);
        }

        // Панель действий
        if (actionsPanel != null) {
            actionsPanel.setBackground(AppTheme.bgPanel());
            updateBorderColors(actionsPanel);
            // Кнопки внутри actionsPanel
            for (Component c : actionsPanel.getComponents()) {
                if (c instanceof AbstractButton btn) {
                    btn.setBackground(AppTheme.bgPanel());
                    btn.setForeground(AppTheme.text());
                    updateBorderColors(btn);
                }
            }
        }

        // Счётчик
        if (counterLabel != null)
            counterLabel.setForeground(AppTheme.textSecondary());

        // Кнопка «Очистить» — особый стиль (красная)
        if (clearBtn != null) {
            clearBtn.setBackground(new Color(180, 60, 60));
            clearBtn.setForeground(Color.WHITE);
            clearBtn.setBorder(new CompoundBorder(
                    new LineBorder(new Color(150, 40, 40), 1, true),
                    new EmptyBorder(2, 10, 2, 10)
            ));
        }

        // Кнопка сортировки — цвет зависит от активности сортировки
        if (sortBtn != null) {
            sortBtn.setBackground(currentSort != null ? AppTheme.filterActive() : AppTheme.bgPanel());
            sortBtn.setBorder(new CompoundBorder(
                    new LineBorder(AppTheme.border(), 1, true),
                    new EmptyBorder(0, 0, 0, 0)
            ));
        }

        // Кнопка фильтров — цвет зависит от активности фильтра
        if (filterBtn != null) {
            boolean hasFilters = host.getFilterChain() != null && !host.getFilterChain().isEmpty();
            filterBtn.setBackground(hasFilters ? AppTheme.filterActive() : AppTheme.bgPanel());
            filterBtn.setForeground(AppTheme.text());
            updateBorderColors(filterBtn);
        }

        // Поле поиска
        if (searchField != null) {
            searchField.setBackground(AppTheme.bgPanel());
            searchField.setForeground(searchField.getText().equals(SEARCH_PLACEHOLDER)
                    ? AppTheme.textSecondary() : AppTheme.text());
            searchField.setCaretColor(AppTheme.text());
        }
    }

    /** Обновляет цвет бордера компонента при смене темы */
    private void updateBorderColors(JComponent comp) {
        if (comp == null) return;
        javax.swing.border.Border border = comp.getBorder();
        if (border instanceof javax.swing.border.TitledBorder tb) {
            tb.setTitleColor(AppTheme.text());
            if (tb.getBorder() instanceof CompoundBorder inner
                    && inner.getOutsideBorder() instanceof LineBorder) {
                tb.setBorder(new CompoundBorder(
                        new LineBorder(AppTheme.border(), 1, true),
                        new EmptyBorder(6, 6, 6, 6)
                ));
            }
        } else if (border instanceof LineBorder) {
            comp.setBorder(new LineBorder(AppTheme.border(), 1, true));
        } else if (border instanceof CompoundBorder cb) {
            javax.swing.border.Border outside = cb.getOutsideBorder();
            javax.swing.border.Border inside  = cb.getInsideBorder();
            if (outside instanceof LineBorder) {
                comp.setBorder(new CompoundBorder(
                        new LineBorder(AppTheme.border(), 1, true), inside));
            }
        }
    }

    // -------------------------------------------------------------------------
    // Построение UI
    // -------------------------------------------------------------------------

    private void buildUI() {
        // Список миссий (прокручиваемый)
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
        buttonPanel.setBackground(AppTheme.bgPanel());
        buttonPanel.setBorder(new EmptyBorder(8, 8, 8, 8));

        missionScroll = new JScrollPane(buttonPanel);
        styleScrollPane(missionScroll);
        missionScroll.setBorder(createTitledBorder("Миссии"));
        missionScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        missionScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

        add(missionScroll, BorderLayout.CENTER);
        bottomPanel = buildBottomPanel();
        add(bottomPanel, BorderLayout.SOUTH);
    }

    /** Нижняя часть: счётчик, поиск/фильтры, статистика, помощник */
    private JPanel buildBottomPanel() {
        counterLabel = new JLabel(" ");
        counterLabel.setFont(UI_FONT);
        counterLabel.setForeground(AppTheme.textSecondary());

        clearBtn = buildClearButton();

        JPanel counterRow = new JPanel(new BorderLayout(6, 0));
        counterRow.setOpaque(false);
        counterRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        counterRow.setBorder(new EmptyBorder(4, 6, 4, 6));
        counterRow.add(counterLabel, BorderLayout.CENTER);
        counterRow.add(clearBtn, BorderLayout.EAST);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(AppTheme.bgMain());
        searchPanel = buildSearchPanel();
        actionsPanel = buildActionsPanel();
        panel.add(counterRow);
        panel.add(searchPanel);
        panel.add(Box.createVerticalStrut(4));
        panel.add(actionsPanel);
        return panel;
    }

    /** Панель поиска + фильтры + сортировка */
    private JPanel buildSearchPanel() {
        JPanel searchPanel = new JPanel();
        searchPanel.setLayout(new BoxLayout(searchPanel, BoxLayout.Y_AXIS));
        searchPanel.setBackground(AppTheme.bgPanel());
        searchPanel.setBorder(createTitledBorder("Поиск"));

        searchField = new JTextField();
        searchField.setFont(UI_FONT);
        searchField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        searchField.setToolTipText("Поиск по ID, локации, результату, проклятию");
        searchField.setText(SEARCH_PLACEHOLDER);
        searchField.setForeground(AppTheme.textSecondary());

        searchField.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusGained(java.awt.event.FocusEvent e) {
                if (searchField.getText().equals(SEARCH_PLACEHOLDER)) {
                    searchField.setText("");
                    searchField.setForeground(AppTheme.text());
                }
            }
            @Override public void focusLost(java.awt.event.FocusEvent e) {
                if (searchField.getText().isEmpty()) {
                    searchField.setText(SEARCH_PLACEHOLDER);
                    searchField.setForeground(AppTheme.textSecondary());
                }
            }
        });

        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e)  { applySearchAndFilter(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e)  { applySearchAndFilter(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { applySearchAndFilter(); }
        });

        filterBtn = createSecondaryButton("Фильтры");
        filterBtn.setToolTipText("Настроить фильтры");
        filterBtn.addActionListener(e -> {
            FilterDialog dialog = new FilterDialog(host.getFrame(), host.getFilterChain(), host.getFavorites());
            dialog.setVisible(true);
            if (dialog.isApplied()) {
                filterBtn.setBackground(host.getFilterChain().isEmpty()
                        ? AppTheme.bgSecondary() : AppTheme.filterActive());
                filterBtn.setOpaque(true);
                applySearchAndFilter();
            }
        });

        sortBtn = buildSortButton();

        JPanel filterRow = new JPanel(new BorderLayout(6, 0));
        filterRow.setOpaque(false);
        filterRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        filterRow.add(filterBtn, BorderLayout.CENTER);
        filterRow.add(sortBtn,   BorderLayout.EAST);

        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.setOpaque(false);
        inner.setBorder(new EmptyBorder(6, 4, 6, 4));
        inner.add(searchField);
        inner.add(Box.createVerticalStrut(6));
        inner.add(filterRow);

        searchPanel.add(inner);
        return searchPanel;
    }

    /** Панель действий: «Общая статистика» + кнопка AI-помощника */
    private JPanel buildActionsPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(AppTheme.bgPanel());
        panel.setBorder(new CompoundBorder(
                new LineBorder(AppTheme.border(), 1, true),
                new EmptyBorder(10, 10, 10, 10)
        ));

        JButton statsButton = createSecondaryWideButton("Общая статистика");
        statsButton.addActionListener(e -> {
            if (host.getMissions().isEmpty()) {
                showWarn("Сначала загрузите файлы миссий.");
                return;
            }
            // Снимаем выделение с миссии — активна статистика
            if (activeButton != null) resetButtonStyle(activeButton);
            activeButton = statsButton;
            statsButton.setBackground(AppTheme.bgSelected());
            statsButton.setBorder(new CompoundBorder(
                    new LineBorder(AppTheme.borderSelected(), 1, true),
                    new EmptyBorder(10, 14, 10, 14)
            ));
            host.onShowStats();
        });
        statsButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton assistantBtn = buildAssistantButton();
        assistantBtn.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(statsButton);
        panel.add(Box.createVerticalStrut(12));
        panel.add(assistantBtn);
        return panel;
    }

    // -------------------------------------------------------------------------
    // Список миссий — построение и обновление
    // -------------------------------------------------------------------------

    private void applySearchAndFilter() {
        String query = searchField != null ? searchField.getText() : "";
        if (query.equals(SEARCH_PLACEHOLDER)) query = "";

        List<Mission> filtered = host.getFilterChain().applyWithSearch(host.getMissions(), query);

        if (currentSort != null) {
            filtered = new ArrayList<>(filtered);
            filtered.sort(currentSort);
        }

        rebuildFromFiltered(filtered);
    }

    private void rebuildFromFiltered(List<Mission> missions) {
        buttonPanel.removeAll();
        missionButtons.clear();  // перестраиваем карту под актуальный список

        for (Mission mission : missions) {
            String label = mission.getMissionId() != null ? mission.getMissionId() : "Миссия";
            JButton btn = createMissionButton(label);
            missionButtons.put(mission, btn);  // регистрируем кнопку

            int realIdx = host.getMissions().indexOf(mission);
            btn.addActionListener(e -> {
                setActiveButton(btn);
                host.onMissionSelected(realIdx);
            });

            JButton starBtn = buildStarButton(mission);

            JPanel row = new JPanel(new BorderLayout(4, 0));
            row.setOpaque(false);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
            row.add(btn,    BorderLayout.CENTER);
            row.add(starBtn, BorderLayout.EAST);

            buttonPanel.add(row);
            buttonPanel.add(Box.createVerticalStrut(8));
        }

        // Обновляем счётчик
        if (counterLabel != null) {
            int total = host.getMissions().size();
            counterLabel.setText(total > 0
                    ? "Найдено: " + missions.size() + " из " + total
                    : " ");
        }
        if (clearBtn != null) clearBtn.setVisible(!host.getMissions().isEmpty());

        buttonPanel.revalidate();
        buttonPanel.repaint();
    }

    private void setActiveButton(JButton btn) {
        if (activeButton != null) resetButtonStyle(activeButton);
        activeButton = btn;
        btn.setBackground(AppTheme.bgSelected());
        btn.setBorder(new CompoundBorder(
                new LineBorder(AppTheme.borderSelected(), 1, true),
                new EmptyBorder(10, 14, 10, 14)
        ));
        btn.setOpaque(true);
    }

    private void resetButtonStyle(JButton btn) {
        btn.setBackground(AppTheme.bgPanel());
        btn.setForeground(AppTheme.text());
        btn.setBorder(new CompoundBorder(
                new LineBorder(AppTheme.border(), 1, true),
                new EmptyBorder(10, 14, 10, 14)
        ));
        btn.setOpaque(true);
    }

    // -------------------------------------------------------------------------
    // Очистка миссий
    // -------------------------------------------------------------------------

    private void confirmClearMissions() {
        int choice = JOptionPane.showConfirmDialog(
                host.getFrame(),
                "Очистить все загруженные миссии?\n\n"
                        + "Внимание: фильтры и сортировка будут\n"
                        + "сброшены в значения по умолчанию.",
                "Очистка миссий",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (choice != JOptionPane.YES_OPTION) return;

        // Сброс внутреннего состояния панели
        host.getFilterChain().clear();
        currentSort     = null;
        currentSortName = null;
        if (sortBtn != null) sortBtn.setBackground(AppTheme.bgPanel());
        if (searchField != null) {
            searchField.setText(SEARCH_PLACEHOLDER);
            searchField.setForeground(AppTheme.textSecondary());
        }
        activeButton = null;

        // Делегируем очистку данных хосту
        host.onClearConfirmed();
    }

    // -------------------------------------------------------------------------
    // Кнопки-звёздочки (избранное)
    // -------------------------------------------------------------------------

    private JButton buildStarButton(Mission mission) {
        boolean isFav = host.getFavorites().contains(mission);
        ImageIcon filledIcon  = loadIcon("/templates/assets/star_filled.png",  18);
        ImageIcon outlineIcon = loadIcon("/templates/assets/star_outline.png", 18);

        JButton btn = new JButton();
        applyStarStyle(btn, isFav, filledIcon, outlineIcon);
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(true);
        btn.setOpaque(true);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(38, 42));
        btn.setToolTipText(isFav ? "Убрать из избранного" : "Добавить в избранное");

        btn.addActionListener(e -> {
            Set<Mission> favs = host.getFavorites();
            boolean nowFav = !favs.contains(mission);
            if (nowFav) favs.add(mission); else favs.remove(mission);
            applyStarStyle(btn, nowFav, filledIcon, outlineIcon);
            btn.setToolTipText(nowFav ? "Убрать из избранного" : "Добавить в избранное");
            applySearchAndFilter();
        });

        return btn;
    }

    private void applyStarStyle(JButton btn, boolean isFav, ImageIcon filled, ImageIcon outline) {
        ImageIcon icon = isFav ? filled : outline;
        if (icon != null) {
            btn.setIcon(icon);
            btn.setText("");
        } else {
            btn.setIcon(null);
            btn.setText(isFav ? "★" : "☆");
            btn.setFont(new Font("Segoe UI", Font.BOLD, 18));
            btn.setForeground(isFav ? new Color(220, 170, 30) : AppTheme.textSecondary());
        }
        btn.setBackground(AppTheme.bgPanel());
        btn.setBorder(new CompoundBorder(
                new LineBorder(AppTheme.border(), 1, true),
                new EmptyBorder(0, 0, 0, 0)
        ));
    }

    private ImageIcon loadIcon(String resourcePath, int size) {
        try {
            java.net.URL url = getClass().getResource(resourcePath);
            if (url != null) {
                ImageIcon original = new ImageIcon(url);
                Image scaled = original.getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH);
                return new ImageIcon(scaled);
            }
        } catch (Exception ignored) {}
        return null;
    }

    // -------------------------------------------------------------------------
    // Кнопка очистки
    // -------------------------------------------------------------------------

    private JButton buildClearButton() {
        ImageIcon icon = null;
        try {
            java.net.URL url = getClass().getResource("/templates/assets/clear.png");
            if (url != null) {
                ImageIcon orig = new ImageIcon(url);
                Image scaled = orig.getImage().getScaledInstance(14, 14, Image.SCALE_SMOOTH);
                icon = new ImageIcon(scaled);
            }
        } catch (Exception ignored) {}

        JButton btn = new JButton();
        if (icon != null) btn.setIcon(icon); else btn.setText("✕");
        btn.setBackground(new Color(180, 60, 60));
        btn.setForeground(Color.WHITE);
        btn.setOpaque(true);
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(true);
        btn.setBorder(new CompoundBorder(
                new LineBorder(new Color(150, 40, 40), 1, true),
                new EmptyBorder(2, 10, 2, 10)
        ));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setToolTipText("Очистить все миссии");
        btn.setVisible(false);
        btn.addActionListener(e -> confirmClearMissions());
        return btn;
    }

    // -------------------------------------------------------------------------
    // Кнопка сортировки
    // -------------------------------------------------------------------------

    private JButton buildSortButton() {
        ImageIcon icon = loadIcon("/templates/assets/sort.png", 18);
        JButton btn = new JButton();
        if (icon != null) btn.setIcon(icon); else btn.setText("↕");
        btn.setBackground(AppTheme.bgPanel());
        btn.setOpaque(true);
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(true);
        btn.setBorder(new CompoundBorder(
                new LineBorder(AppTheme.border(), 1, true),
                new EmptyBorder(0, 0, 0, 0)
        ));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(36, 36));
        btn.setToolTipText("Сортировка миссий");
        btn.addActionListener(e -> showSortPopup(btn));
        return btn;
    }

    private void showSortPopup(JButton anchor) {
        JPopupMenu popup = new JPopupMenu();
        addSortItem(popup, "По дате (новые ↑)",
                Comparator.comparing((Mission m) -> m.getDate() != null ? m.getDate() : "").reversed());
        addSortItem(popup, "По дате (старые ↑)",
                Comparator.comparing((Mission m) -> m.getDate() != null ? m.getDate() : ""));
        addSortItem(popup, "По угрозе (опасные ↑)",
                Comparator.comparingInt((Mission m) -> threatOrder(m)).reversed());
        addSortItem(popup, "По ущербу (дорогие ↑)",
                Comparator.comparingDouble(Mission::getDamageCost).reversed());
        addSortItem(popup, "По имени (A→Z)",
                Comparator.comparing((Mission m) -> m.getMissionId() != null ? m.getMissionId() : ""));

        popup.addSeparator();
        JMenuItem reset = new JMenuItem("Сбросить сортировку");
        reset.setFont(UI_FONT);
        reset.addActionListener(e -> {
            currentSort     = null;
            currentSortName = null;
            sortBtn.setBackground(AppTheme.bgPanel());
            sortBtn.setToolTipText("Сортировка миссий");
            applySearchAndFilter();
        });
        popup.add(reset);
        popup.show(anchor, 0, anchor.getHeight());
    }

    private void addSortItem(JPopupMenu popup, String name, Comparator<Mission> comparator) {
        JMenuItem item = new JMenuItem(name);
        item.setFont(name.equals(currentSortName) ? UI_BOLD : UI_FONT);
        if (name.equals(currentSortName)) { item.setBackground(AppTheme.filterActive()); item.setOpaque(true); }
        item.addActionListener(e -> {
            currentSort     = comparator;
            currentSortName = name;
            sortBtn.setBackground(AppTheme.filterActive());
            sortBtn.setToolTipText("Сортировка: " + name);
            applySearchAndFilter();
        });
        popup.add(item);
    }

    private int threatOrder(Mission m) {
        if (m.getCurse() == null || m.getCurse().getThreatLevel() == null) return 0;
        return switch (m.getCurse().getThreatLevel()) {
            case "LOW"          -> 1;
            case "MEDIUM"       -> 2;
            case "HIGH"         -> 3;
            case "SPECIAL_GRADE"-> 4;
            default             -> 0;
        };
    }

    // -------------------------------------------------------------------------
    // Кнопка AI-помощника
    // -------------------------------------------------------------------------

    private JButton buildAssistantButton() {
        ImageIcon icon = loadIcon("/templates/assets/Assistant-stand.png", 88);
        JButton btn = icon != null ? new JButton(icon) : new JButton("AI");
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setToolTipText("Открыть помощника");
        btn.addActionListener(e -> {
            if (host.getCurrentMission() == null && host.getMissions().isEmpty()) {
                showWarn("Сначала загрузите миссии.");
                return;
            }
            new AiAssistantDialog(
                    host.getFrame(), host.getAiService(),
                    host.getCurrentMission(), host.getMissions(),
                    host.getAssistantSleepIcon(), host.getReportText()
            ).show();
        });
        return btn;
    }

    // -------------------------------------------------------------------------
    // Фабричные методы кнопок (локальные копии — не зависят от MainFrame)
    // -------------------------------------------------------------------------

    private JButton createPrimaryButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(UI_BOLD);
        btn.setForeground(AppTheme.text());
        btn.setBackground(AppTheme.bgPanel());
        btn.setFocusPainted(false);
        btn.setBorder(new CompoundBorder(
                new LineBorder(AppTheme.border(), 1, true),
                new EmptyBorder(8, 16, 8, 16)
        ));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(btn.getPreferredSize().width, 38));
        return btn;
    }

    private JButton createSecondaryButton(String text) {
        JButton btn = createPrimaryButton(text);
        btn.setBackground(AppTheme.bgSecondary());
        return btn;
    }

    private JButton createSecondaryWideButton(String text) {
        JButton btn = createSecondaryButton(text);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        return btn;
    }

    private JButton createMissionButton(String text) {
        JButton btn = new JButton(text);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setFont(UI_BOLD);
        btn.setForeground(AppTheme.text());
        btn.setBackground(AppTheme.bgPanel());
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new CompoundBorder(
                new LineBorder(AppTheme.border(), 1, true),
                new EmptyBorder(10, 14, 10, 14)
        ));
        return btn;
    }

    // -------------------------------------------------------------------------
    // Стилизация
    // -------------------------------------------------------------------------

    private void styleScrollPane(JScrollPane sp) {
        sp.getViewport().setBackground(AppTheme.bgPanel());
        sp.setBackground(AppTheme.bgPanel());
        sp.setBorder(new LineBorder(AppTheme.border(), 1, true));
    }

    private javax.swing.border.Border createTitledBorder(String title) {
        return javax.swing.BorderFactory.createTitledBorder(
                new CompoundBorder(
                        new LineBorder(AppTheme.border(), 1, true),
                        new EmptyBorder(6, 6, 6, 6)
                ),
                title, 0, 0, TITLE_FONT, AppTheme.text()
        );
    }

    private void showWarn(String msg) {
        JOptionPane.showMessageDialog(host.getFrame(), msg, "Предупреждение", JOptionPane.WARNING_MESSAGE);
    }
}
