import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import ui.MainFrame;

import javax.swing.*;
import java.util.prefs.Preferences;

public class Main {
    public static void main(String[] args) {
        service.AppLogger.getLogs(); // инициализируем перехват до старта UI

        // Устанавливаем тему ДО создания окон — FlatLaf стилизует ВСЕ компоненты
        boolean dark = Preferences.userRoot()
                .node("mission_analyzer_settings")
                .getBoolean("darkTheme", false);
        if (dark) FlatDarkLaf.setup();
        else FlatLightLaf.setup();

        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}