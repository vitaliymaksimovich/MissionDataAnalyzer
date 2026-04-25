package ui;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StyledReportPane extends JTextPane {

    // --- цвета подсветки (адаптируются к теме) ---

    private static Color sectionColor() {
        return AppTheme.isDark() ? new Color(100, 180, 120) : new Color(45, 90, 55);
    }
    private static Color labelColor() {
        return AppTheme.isDark() ? new Color(120, 150, 180) : new Color(80, 100, 120);
    }

    // Яркие акценты — одинаково читаемы на обоих фонах
    private static final Color COLOR_MISSING = new Color(140, 140, 148);
    private static final Color COLOR_WARNING = new Color(220, 70, 70);
    private static final Color COLOR_SUCCESS = new Color(60, 175, 90);
    private static final Color COLOR_FAILURE = new Color(210, 60, 60);
    private static final Color COLOR_PARTIAL = new Color(215, 140, 30);
    private static final Color COLOR_SPECIAL = new Color(200, 50, 50);
    private static final Color COLOR_HIGH    = new Color(220, 110, 30);
    private static final Color COLOR_MEDIUM  = new Color(200, 170, 30);
    private static final Color COLOR_LOW     = new Color(60, 175, 90);

    private static final Map<Pattern, Color> WORD_PATTERNS = new HashMap<>();

    static {
        WORD_PATTERNS.put(Pattern.compile("\\bSUCCESS\\b"), COLOR_SUCCESS);
        WORD_PATTERNS.put(Pattern.compile("\\bFAILURE\\b"), COLOR_FAILURE);
        WORD_PATTERNS.put(Pattern.compile("\\bPARTIAL_SUCCESS\\b"), COLOR_PARTIAL);
        WORD_PATTERNS.put(Pattern.compile("\\bSPECIAL_GRADE\\b"), COLOR_SPECIAL);
        WORD_PATTERNS.put(Pattern.compile("\\bHIGH\\b"), COLOR_HIGH);
        WORD_PATTERNS.put(Pattern.compile("\\bMEDIUM\\b"), COLOR_MEDIUM);
        WORD_PATTERNS.put(Pattern.compile("\\bLOW\\b"), COLOR_LOW);
        WORD_PATTERNS.put(Pattern.compile("\\[не указано\\]"), COLOR_MISSING);
        WORD_PATTERNS.put(Pattern.compile("\\[ВНИМАНИЕ\\][^\\n]*"), COLOR_WARNING);
    }

    private int reportFontSize = 15;
    private String reportFontFamily = "Consolas";

    public StyledReportPane() {
        setEditable(false);
        setOpaque(true);
    }

    public void setReportFontSize(int size) {
        if (size > 0) {
            this.reportFontSize = size;
        }
    }

    public int getReportFontSize() {
        return reportFontSize;
    }

    public void setReportFontFamily(String family) {
        if (family != null && !family.isBlank()) {
            this.reportFontFamily = family;
        }
    }

    public String getReportFontFamily() {
        return reportFontFamily;
    }

    public void setStyledText(String text) {
        StyledDocument doc = new DefaultStyledDocument();

        Style base = doc.addStyle("base", null);
        StyleConstants.setFontFamily(base, reportFontFamily);
        StyleConstants.setFontSize(base, reportFontSize);
        StyleConstants.setForeground(base, AppTheme.text());

        try {
            doc.insertString(0, text, base);
        } catch (BadLocationException e) {
            setText(text);
            return;
        }

        applyPattern(doc, Pattern.compile("=== .+ ==="), sectionColor(), true, false);
        applyPattern(doc, Pattern.compile("(?m)^  [\\wА-Яа-яЁё ]+: "), labelColor(), false, false);

        for (Map.Entry<Pattern, Color> entry : WORD_PATTERNS.entrySet()) {
            applyPattern(doc, entry.getKey(), entry.getValue(), true, false);
        }

        setDocument(doc);
        setCaretPosition(0);
    }

    private void applyPattern(StyledDocument doc, Pattern pattern,
                              Color color, boolean bold, boolean italic) {
        try {
            String text = doc.getText(0, doc.getLength());
            Matcher matcher = pattern.matcher(text);
            while (matcher.find()) {
                Style style = doc.addStyle(null, null);
                StyleConstants.setFontFamily(style, reportFontFamily);
                StyleConstants.setFontSize(style, reportFontSize);
                StyleConstants.setForeground(style, color);
                StyleConstants.setBold(style, bold);
                StyleConstants.setItalic(style, italic);
                doc.setCharacterAttributes(
                        matcher.start(),
                        matcher.end() - matcher.start(),
                        style,
                        false
                );
            }
        } catch (BadLocationException ignored) {
        }
    }
}