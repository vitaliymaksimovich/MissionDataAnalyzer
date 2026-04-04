import ui.MainFrame;
import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        service.AppLogger.getLogs(); // инициализируем перехват до старта UI
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}