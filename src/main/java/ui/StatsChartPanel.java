package ui;

import model.Mission;
import model.Sorcerer;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Arc2D;
import java.util.*;
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
        setBackground(AppTheme.bgMain());
        setPreferredSize(new Dimension(1100, 420));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        int third = w / 3;

        // 1. Круговая диаграмма — итоги миссий (с процентами)
        drawPieChart(g2, 0, 0, third, h,
                "Итоги миссий",
                missions.stream().collect(Collectors.groupingBy(
                        m -> m.getOutcome() != null ? m.getOutcome() : "UNKNOWN",
                        Collectors.counting()
                ))
        );

        // 2. Столбчатая диаграмма — уровни угроз
        drawBarChart(g2, third, 0, third, h,
                "Уровни угроз",
                missions.stream()
                        .filter(m -> m.getCurse() != null && m.getCurse().getThreatLevel() != null)
                        .collect(Collectors.groupingBy(
                                m -> m.getCurse().getThreatLevel(),
                                Collectors.counting()
                        ))
        );

        // 3. Горизонтальная диаграмма — участники
        Map<String, Long> sorcererCounts = missions.stream()
                .flatMap(m -> m.getSorcerers().stream())
                .map(Sorcerer::getName)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(n -> n, Collectors.counting()));

        drawHorizontalBarChart(g2, third * 2, 0, w - third * 2, h,
                "Участники", sorcererCounts);
    }

    private void drawPieChart(Graphics2D g2, int x, int y, int w, int h,
                              String title, Map<String, Long> data) {
        if (data.isEmpty()) return;

        g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
        g2.setColor(AppTheme.text());
        g2.drawString(title, x + w / 2 - g2.getFontMetrics().stringWidth(title) / 2, y + 22);

        int padding = 40;
        int size = Math.min(w, h) - padding * 2 - 60;
        int cx = x + w / 2 - size / 2;
        int cy = y + padding + 20;

        long total = data.values().stream().mapToLong(Long::longValue).sum();
        double startAngle = 0;
        int colorIdx = 0;

        // секторы + запоминаем углы для процентов
        List<double[]> sectors = new ArrayList<>();
        for (Map.Entry<String, Long> entry : data.entrySet()) {
            double angle = 360.0 * entry.getValue() / total;
            g2.setColor(CHART_COLORS[colorIdx % CHART_COLORS.length]);
            g2.fill(new Arc2D.Double(cx, cy, size, size, startAngle, angle, Arc2D.PIE));
            g2.setColor(AppTheme.bgMain());
            g2.draw(new Arc2D.Double(cx, cy, size, size, startAngle, angle, Arc2D.PIE));
            sectors.add(new double[]{startAngle, angle, entry.getValue()});
            startAngle += angle;
            colorIdx++;
        }

        // проценты на секторах
        g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
        for (double[] sector : sectors) {
            int pct = (int) Math.round(100.0 * sector[2] / total);
            if (pct < 5) continue; // слишком узкий сектор

            double midAngle = Math.toRadians(sector[0] + sector[1] / 2);
            double radius = size / 2.0 * 0.65;
            int lx = (int) (cx + size / 2.0 + radius * Math.cos(midAngle));
            int ly = (int) (cy + size / 2.0 - radius * Math.sin(midAngle));

            String label = pct + "%";
            FontMetrics fm = g2.getFontMetrics();
            g2.setColor(Color.WHITE);
            g2.drawString(label, lx - fm.stringWidth(label) / 2, ly + fm.getAscent() / 2);
        }

        // легенда
        int legendY = cy + size + 10;
        colorIdx = 0;
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        int legendX = x + 10;
        for (Map.Entry<String, Long> entry : data.entrySet()) {
            g2.setColor(CHART_COLORS[colorIdx % CHART_COLORS.length]);
            g2.fillRect(legendX, legendY, 12, 12);
            g2.setColor(AppTheme.text());
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
        g2.setColor(AppTheme.text());
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

            g2.setColor(AppTheme.text());
            String val = String.valueOf(entry.getValue());
            g2.drawString(val, barX + barWidth / 2 - g2.getFontMetrics().stringWidth(val) / 2,
                    barY - 4);

            String label = entry.getKey();
            if (label.length() > 8) label = label.substring(0, 8) + "\u2026";
            g2.drawString(label, barX + barWidth / 2 - g2.getFontMetrics().stringWidth(label) / 2,
                    chartY + chartH + 14);

            barX += barWidth + 12;
            colorIdx++;
        }

        // ось X
        g2.setColor(AppTheme.border());
        g2.drawLine(chartX, chartY + chartH, chartX + chartW, chartY + chartH);
    }

    private void drawHorizontalBarChart(Graphics2D g2, int x, int y, int w, int h,
                                        String title, Map<String, Long> data) {
        if (data.isEmpty()) {
            g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
            g2.setColor(AppTheme.text());
            g2.drawString(title, x + w / 2 - g2.getFontMetrics().stringWidth(title) / 2, y + 22);
            g2.setFont(new Font("Segoe UI", Font.ITALIC, 11));
            g2.setColor(AppTheme.textSecondary());
            g2.drawString("Нет данных", x + w / 2 - 30, y + h / 2);
            return;
        }

        g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
        g2.setColor(AppTheme.text());
        g2.drawString(title, x + w / 2 - g2.getFontMetrics().stringWidth(title) / 2, y + 22);

        // сортируем по убыванию, берём топ-8
        List<Map.Entry<String, Long>> sorted = data.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(8)
                .toList();

        long max = sorted.getFirst().getValue();
        int padding = 16;
        int chartY = y + 42;
        int chartH = h - 60;

        int barCount = sorted.size();
        int barHeight = Math.min(26, (chartH - 10) / barCount - 6);

        // вычисляем ширину для имён
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        FontMetrics fm = g2.getFontMetrics();
        int nameWidth = 0;
        for (Map.Entry<String, Long> entry : sorted) {
            String name = truncate(entry.getKey(), 16);
            nameWidth = Math.max(nameWidth, fm.stringWidth(name));
        }
        nameWidth += 8; // отступ после имени

        int barStartX = x + padding + nameWidth;
        int barMaxW = w - padding * 2 - nameWidth - 30;
        int currentY = chartY;
        int colorIdx = 0;

        for (Map.Entry<String, Long> entry : sorted) {
            String name = truncate(entry.getKey(), 16);
            int textY = currentY + barHeight / 2 + fm.getAscent() / 2 - 1;

            // имя
            g2.setColor(AppTheme.text());
            g2.drawString(name, x + padding, textY);

            // полоска
            int barW = Math.max(4, (int) (barMaxW * entry.getValue() / max));
            g2.setColor(CHART_COLORS[colorIdx % CHART_COLORS.length]);
            g2.fillRoundRect(barStartX, currentY, barW, barHeight, 6, 6);

            // число справа
            g2.setColor(AppTheme.text());
            g2.drawString(String.valueOf(entry.getValue()), barStartX + barW + 6, textY);

            currentY += barHeight + 6;
            colorIdx++;
        }
    }

    private String truncate(String s, int maxLen) {
        return s.length() > maxLen ? s.substring(0, maxLen - 1) + "\u2026" : s;
    }
}
