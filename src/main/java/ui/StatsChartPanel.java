package ui;

import model.Mission;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Arc2D;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class StatsChartPanel extends JPanel {

    private final List<Mission> missions;

    private static final Color[] CHART_COLORS = {
            new Color(76, 153, 90),
            new Color(196, 64, 64),
            new Color(210, 140, 30),
            new Color(70, 130, 180),
            new Color(150, 90, 170),
            new Color(80, 170, 160)
    };

    public StatsChartPanel(List<Mission> missions) {
        this.missions = missions;
        setBackground(new Color(245, 247, 250));
        setPreferredSize(new Dimension(800, 340));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        int half = w / 2;

        drawPieChart(g2, 0, 0, half, h,
                "Итоги миссий",
                missions.stream().collect(Collectors.groupingBy(
                        m -> m.getOutcome() != null ? m.getOutcome() : "UNKNOWN",
                        Collectors.counting()
                ))
        );

        drawBarChart(g2, half, 0, half, h,
                "Уровни угроз",
                missions.stream()
                        .filter(m -> m.getCurse() != null && m.getCurse().getThreatLevel() != null)
                        .collect(Collectors.groupingBy(
                                m -> m.getCurse().getThreatLevel(),
                                Collectors.counting()
                        ))
        );
    }

    private void drawPieChart(Graphics2D g2, int x, int y, int w, int h,
                              String title, Map<String, Long> data) {
        if (data.isEmpty()) return;

        g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
        g2.setColor(new Color(35, 42, 52));
        g2.drawString(title, x + w / 2 - g2.getFontMetrics().stringWidth(title) / 2, y + 22);

        int padding = 40;
        int size = Math.min(w, h) - padding * 2 - 60;
        int cx = x + w / 2 - size / 2;
        int cy = y + padding + 20;

        long total = data.values().stream().mapToLong(Long::longValue).sum();
        double startAngle = 0;
        int colorIdx = 0;

        for (Map.Entry<String, Long> entry : data.entrySet()) {
            double angle = 360.0 * entry.getValue() / total;
            g2.setColor(CHART_COLORS[colorIdx % CHART_COLORS.length]);
            g2.fill(new Arc2D.Double(cx, cy, size, size, startAngle, angle, Arc2D.PIE));
            g2.setColor(Color.WHITE);
            g2.draw(new Arc2D.Double(cx, cy, size, size, startAngle, angle, Arc2D.PIE));
            startAngle += angle;
            colorIdx++;
        }

        // легенда
        int legendY = cy + size + 10;
        colorIdx = 0;
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        int legendX = x + 10;
        for (Map.Entry<String, Long> entry : data.entrySet()) {
            g2.setColor(CHART_COLORS[colorIdx % CHART_COLORS.length]);
            g2.fillRect(legendX, legendY, 12, 12);
            g2.setColor(new Color(35, 42, 52));
            g2.drawString(entry.getKey() + " (" + entry.getValue() + ")",
                    legendX + 16, legendY + 11);
            legendX += g2.getFontMetrics().stringWidth(
                    entry.getKey() + " (" + entry.getValue() + ")") + 28;
            if (legendX > x + w - 60) { legendX = x + 10; legendY += 18; }
            colorIdx++;
        }
    }

    private void drawBarChart(Graphics2D g2, int x, int y, int w, int h,
                              String title, Map<String, Long> data) {
        if (data.isEmpty()) return;

        g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
        g2.setColor(new Color(35, 42, 52));
        g2.drawString(title, x + w / 2 - g2.getFontMetrics().stringWidth(title) / 2, y + 22);

        int padding = 40;
        int chartX = x + padding;
        int chartY = y + 35;
        int chartW = w - padding * 2;
        int chartH = h - 100;

        long max = data.values().stream().mapToLong(Long::longValue).max().orElse(1);

        int barCount = data.size();
        int barWidth = Math.max(20, chartW / barCount - 12);
        int barX = chartX + 10;
        int colorIdx = 0;

        g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        for (Map.Entry<String, Long> entry : data.entrySet()) {
            int barH = (int) (chartH * entry.getValue() / max);
            int barY = chartY + chartH - barH;

            g2.setColor(CHART_COLORS[colorIdx % CHART_COLORS.length]);
            g2.fillRoundRect(barX, barY, barWidth, barH, 6, 6);

            g2.setColor(new Color(35, 42, 52));
            String val = String.valueOf(entry.getValue());
            g2.drawString(val, barX + barWidth / 2 - g2.getFontMetrics().stringWidth(val) / 2,
                    barY - 4);

            String label = entry.getKey();
            if (label.length() > 8) label = label.substring(0, 8) + "…";
            g2.drawString(label, barX + barWidth / 2 - g2.getFontMetrics().stringWidth(label) / 2,
                    chartY + chartH + 14);

            barX += barWidth + 12;
            colorIdx++;
        }

        // ось X
        g2.setColor(new Color(200, 210, 220));
        g2.drawLine(chartX, chartY + chartH, chartX + chartW, chartY + chartH);
    }
}