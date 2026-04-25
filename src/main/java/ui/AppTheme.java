package ui;

import java.awt.Color;

/**
 * Центральный провайдер цветов приложения.
 * Все UI-классы берут цвета отсюда — единый стиль во всех окнах.
 * Цвета адаптируются к текущей теме (светлая / тёмная).
 */
public class AppTheme {

    // --- основные фоны ---

    public static Color bgMain() {
        return isDark() ? new Color(43, 46, 50) : new Color(245, 247, 250);
    }

    public static Color bgPanel() {
        return isDark() ? new Color(50, 53, 58) : Color.WHITE;
    }

    public static Color bgTop() {
        return isDark() ? new Color(47, 50, 54) : new Color(240, 243, 247);
    }

    public static Color bgSelected() {
        return isDark() ? new Color(55, 80, 115) : new Color(210, 226, 248);
    }

    public static Color bgSecondary() {
        return isDark() ? new Color(58, 62, 67) : new Color(233, 239, 247);
    }

    // --- текст ---

    public static Color text() {
        return isDark() ? new Color(210, 215, 222) : new Color(35, 42, 52);
    }

    public static Color textSecondary() {
        return isDark() ? new Color(140, 148, 158) : Color.GRAY;
    }

    // --- границы ---

    public static Color border() {
        return isDark() ? new Color(68, 72, 78) : new Color(210, 216, 224);
    }

    public static Color borderSelected() {
        return isDark() ? new Color(80, 120, 170) : new Color(155, 185, 225);
    }

    // --- акценты для фильтров ---

    public static Color filterActive() {
        return isDark() ? new Color(60, 90, 140) : new Color(180, 210, 255);
    }

    // --- текст для кнопки (в FlatLaf не трогаем стандартные кнопки) ---

    public static Color buttonFg() {
        return text();
    }

    public static boolean isDark() {
        return SettingsDialog.isDarkTheme();
    }
}
