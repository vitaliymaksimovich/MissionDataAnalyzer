package ui;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;

/**
 * Утилита применения темы ко всем компонентам окна.
 * <p>
 * Выделена из MainFrame, чтобы не раздувать главное окно.
 * Содержит только статические методы — состояния нет, объектная модель не нужна.
 * <p>
 * Ответственности:
 * <ul>
 *   <li>Переключение Look-and-Feel (FlatDarkLaf / FlatLightLaf).</li>
 *   <li>Рекурсивный обход дерева компонентов и применение цветов {@link AppTheme}.</li>
 *   <li>Обновление border'ов (LineBorder/CompoundBorder/TitledBorder) под текущую тему.</li>
 * </ul>
 * <p>
 * Важно: у {@link MissionListPanel} есть свой {@code onThemeChanged()} —
 * его мы НЕ трогаем при обходе, панель сама обновит свои внутренние цвета.
 */
public final class ThemeManager {

    private ThemeManager() {}

    /** Применяет FlatLaf-тему и обновляет дерево компонентов окна. */
    public static void applyLookAndFeel(JFrame frame, boolean dark) {
        try {
            if (dark) FlatDarkLaf.setup();
            else      FlatLightLaf.setup();
            SwingUtilities.updateComponentTreeUI(frame);
        } catch (Exception ignored) {
            // LAF может не примениться — оставляем системный
        }
    }

    /**
     * Рекурсивно обходит дерево контейнеров и применяет цвета {@link AppTheme}
     * к панелям, лейблам, кнопкам, скроллам и их border'ам.
     * <p>
     * Пропускает {@link MissionListPanel} — у неё собственный {@code onThemeChanged()}.
     */
    public static void applyThemeToTree(Container container) {
        for (Component c : container.getComponents()) {
            if (c instanceof JPanel panel && !(panel instanceof MissionListPanel)) {
                panel.setBackground(AppTheme.bgMain());
            }
            if (c instanceof JLabel label) {
                label.setForeground(AppTheme.text());
            }
            if (c instanceof AbstractButton button) {
                button.setForeground(AppTheme.text());
                button.setBackground(AppTheme.bgPanel());
            }
            if (c instanceof JComponent jc) {
                updateTitledBorderColor(jc);
                updateLineBorders(jc);
            }
            if (c instanceof JSplitPane split) {
                split.setBackground(AppTheme.bgMain());
                applyThemeToTree(split);
            }
            if (c instanceof JScrollPane scroll) {
                scroll.getViewport().setBackground(AppTheme.bgPanel());
                scroll.setBackground(AppTheme.bgPanel());
                updateTitledBorderColor(scroll);
                updateLineBorders(scroll);
            }
            if (c instanceof Container cont
                    && !(c instanceof MissionListPanel)
                    && !(c instanceof JComboBox)
                    && !(c instanceof JScrollPane)
                    && !(c instanceof JSpinner)) {
                applyThemeToTree(cont);
            }
        }
    }

    /** Обновляет цвет TitledBorder под текущую тему. */
    public static void updateTitledBorderColor(JComponent comp) {
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
        }
    }

    /** Обновляет LineBorder/MatteBorder (в том числе вложенные в CompoundBorder). */
    public static void updateLineBorders(JComponent comp) {
        javax.swing.border.Border border = comp.getBorder();
        if (border instanceof LineBorder) {
            comp.setBorder(new LineBorder(AppTheme.border(), 1, true));
        } else if (border instanceof CompoundBorder cb) {
            javax.swing.border.Border outside = cb.getOutsideBorder();
            javax.swing.border.Border inside  = cb.getInsideBorder();
            boolean changed = false;
            if (outside instanceof MatteBorder mb) {
                Insets i = mb.getBorderInsets(comp);
                outside = new MatteBorder(i.top, i.left, i.bottom, i.right, AppTheme.border());
                changed = true;
            } else if (outside instanceof LineBorder) {
                outside = new LineBorder(AppTheme.border(), 1, true);
                changed = true;
            }
            if (changed) comp.setBorder(new CompoundBorder(outside, inside));
        }
    }
}
