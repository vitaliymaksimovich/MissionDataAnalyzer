package ui;

import parser.plugin.ExportPlugin;
import service.AppLogger;
import service.HtmlReportExporter;
import service.PluginManager;
import service.ReportFormatter;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Контроллер экспорта отчёта — ранее жил внутри MainFrame.
 * <p>
 * Отвечает за:
 * <ul>
 *   <li>Показ всплывающего меню «Экспорт ▼».</li>
 *   <li>Сохранение отчёта в TXT/HTML.</li>
 *   <li>Вызов экспортных плагинов (CSV/Markdown/…).</li>
 * </ul>
 * Все диалоги сохранения привязаны к родительскому окну {@code parent}.
 */
public class ExportController {

    private final JFrame parent;
    private final HtmlReportExporter htmlExporter = new HtmlReportExporter();

    public ExportController(JFrame parent) {
        this.parent = parent;
    }

    /**
     * Показывает popup-меню экспорта около указанного компонента.
     *
     * @param anchor          кнопка/компонент, у которого появится меню
     * @param reportText      текст текущего отчёта
     * @param currentFormatter текущий выбранный форматтер (нужен для заголовка TXT)
     */
    public void showExportMenu(Component anchor, String reportText, ReportFormatter currentFormatter) {
        JPopupMenu menu = new JPopupMenu();

        JMenuItem txtItem  = new JMenuItem("Экспорт TXT");
        JMenuItem htmlItem = new JMenuItem("Экспорт HTML");
        txtItem.addActionListener(e  -> saveToTxt(reportText, currentFormatter));
        htmlItem.addActionListener(e -> saveToHtml(reportText));
        menu.add(txtItem);
        menu.add(htmlItem);

        // Подтягиваем экспортные плагины, если есть
        List<ExportPlugin> plugins = PluginManager.getInstance().getActiveExportPlugins();
        if (!plugins.isEmpty()) {
            menu.addSeparator();
            for (ExportPlugin plugin : plugins) {
                JMenuItem item = new JMenuItem(plugin.getMetadata().name());
                item.addActionListener(ev -> exportWithPlugin(plugin, reportText));
                menu.add(item);
            }
        }

        menu.show(anchor, 0, anchor.getHeight());
    }

    // -------------------------------------------------------------------------
    // Внутренние операции экспорта
    // -------------------------------------------------------------------------

    private void saveToTxt(String reportText, ReportFormatter formatter) {
        if (reportText == null || reportText.isBlank()) { warn("Сначала выполните анализ."); return; }
        JFileChooser fc = new JFileChooser();
        if (fc.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) return;
        File file = ensureExtension(fc.getSelectedFile(), ".txt");

        String sep = "-".repeat(60);
        String ts  = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"));
        String content = "MISSION ANALYZER REPORT\n" + sep + "\n"
                + "Дата анализа: " + ts + "\n"
                + "Тип отчёта:   " + (formatter != null ? formatter.getDisplayName() : "—") + "\n"
                + sep + "\n\n" + reportText + "\n" + sep + "\n";

        try (FileWriter fw = new FileWriter(file, StandardCharsets.UTF_8)) {
            fw.write(content);
            AppLogger.export("TXT", file.getAbsolutePath());
            info("Отчёт сохранён:\n" + file.getAbsolutePath());
        } catch (IOException e) {
            AppLogger.error("Не удалось сохранить TXT: " + e.getMessage());
            error("Не удалось сохранить:\n" + e.getMessage());
        }
    }

    private void saveToHtml(String reportText) {
        if (reportText == null || reportText.isBlank()) { warn("Сначала выполните анализ."); return; }
        JFileChooser fc = new JFileChooser();
        if (fc.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) return;
        File file = ensureExtension(fc.getSelectedFile(), ".html");
        try {
            htmlExporter.export(file, reportText);
            AppLogger.export("HTML", file.getAbsolutePath());
            info("HTML сохранён:\n" + file.getAbsolutePath());
        } catch (IOException e) {
            AppLogger.error("Не удалось сохранить HTML: " + e.getMessage());
            error("Не удалось сохранить:\n" + e.getMessage());
        }
    }

    private void exportWithPlugin(ExportPlugin plugin, String reportText) {
        if (reportText == null || reportText.isBlank()) { warn("Нет отчёта для экспорта."); return; }
        JFileChooser fc = new JFileChooser();
        if (fc.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) return;
        File file = ensureExtension(fc.getSelectedFile(), plugin.getFileExtension());
        try {
            plugin.export(reportText, file);
            AppLogger.export(plugin.getMetadata().name(), file.getAbsolutePath());
            info("Сохранено:\n" + file.getAbsolutePath());
        } catch (Exception e) {
            warn("Ошибка экспорта: " + e.getMessage());
        }
    }

    private File ensureExtension(File file, String ext) {
        return file.getName().toLowerCase().endsWith(ext)
                ? file : new File(file.getAbsolutePath() + ext);
    }

    // -------------------------------------------------------------------------
    // Диалоги
    // -------------------------------------------------------------------------

    private void warn(String msg)  { JOptionPane.showMessageDialog(parent, msg, "Предупреждение", JOptionPane.WARNING_MESSAGE); }
    private void error(String msg) { JOptionPane.showMessageDialog(parent, msg, "Ошибка",          JOptionPane.ERROR_MESSAGE);  }
    private void info(String msg)  { JOptionPane.showMessageDialog(parent, msg, "Успех",           JOptionPane.INFORMATION_MESSAGE); }
}
