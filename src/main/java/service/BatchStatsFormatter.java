package service;

import model.Mission;
import model.Sorcerer;

import java.util.*;
import java.util.stream.Collectors;

public class BatchStatsFormatter {

    public String format(List<Mission> missions) {
        if (missions == null || missions.isEmpty()) return "Нет загруженных миссий.";

        StringBuilder sb = new StringBuilder();
        sb.append("=== СТАТИСТИКА ПО МИССИЯМ ===\n\n");
        sb.append("  Всего миссий: ").append(missions.size()).append("\n");

        // процент успешных миссий с прогресс-баром
        appendSuccessRate(sb, missions);

        // итоги по результатам
        Map<String, Long> outcomes = missions.stream()
                .collect(Collectors.groupingBy(
                        m -> m.getOutcome() != null ? m.getOutcome() : "UNKNOWN",
                        Collectors.counting()
                ));
        sb.append("\n[ ИТОГИ ]\n");
        outcomes.forEach((k, v) -> sb.append("  ").append(k).append(": ").append(v).append("\n"));

        // средний уровень угрозы (числовой)
        appendAverageThreat(sb, missions);

        // уровни угроз
        Map<String, Long> threats = missions.stream()
                .filter(m -> m.getCurse() != null && m.getCurse().getThreatLevel() != null)
                .collect(Collectors.groupingBy(
                        m -> m.getCurse().getThreatLevel(),
                        Collectors.counting()
                ));
        sb.append("\n[ УРОВНИ УГРОЗ ]\n");
        threats.forEach((k, v) -> sb.append("  ").append(k).append(": ").append(v).append("\n"));

        // топ участников
        appendTopSorcerers(sb, missions);

        // общий ущерб
        double totalDamage = missions.stream().mapToDouble(Mission::getDamageCost).sum();
        sb.append("\n[ УЩЕРБ ]\n");
        sb.append("  Суммарный ущерб: ").append(totalDamage).append("\n");
        missions.stream()
                .max((a, b) -> Double.compare(a.getDamageCost(), b.getDamageCost()))
                .ifPresent(m -> sb.append("  Самая дорогая: ").append(m.getMissionId())
                        .append(" (").append(m.getDamageCost()).append(")\n"));

        // локации
        sb.append("\n[ ЛОКАЦИИ ]\n");
        missions.stream()
                .filter(m -> m.getLocation() != null)
                .forEach(m -> sb.append("  - ").append(m.getMissionId())
                        .append(" → ").append(m.getLocation()).append("\n"));

        // гражданские суммарно
        int totalEvacuated = missions.stream()
                .filter(m -> m.getCivilianImpact() != null)
                .mapToInt(m -> m.getCivilianImpact().getEvacuated()).sum();
        int totalInjured = missions.stream()
                .filter(m -> m.getCivilianImpact() != null)
                .mapToInt(m -> m.getCivilianImpact().getInjured()).sum();
        if (totalEvacuated > 0 || totalInjured > 0) {
            sb.append("\n[ ГРАЖДАНСКИЕ (суммарно) ]\n");
            sb.append("  Эвакуировано: ").append(totalEvacuated).append("\n");
            sb.append("  Пострадавших: ").append(totalInjured).append("\n");
        }

        return sb.toString();
    }

    private void appendSuccessRate(StringBuilder sb, List<Mission> missions) {
        long success = missions.stream()
                .filter(m -> "SUCCESS".equals(m.getOutcome()))
                .count();
        int total = missions.size();
        int percent = (int) Math.round(100.0 * success / total);

        int barLength = 20;
        int filled = (int) Math.round(barLength * percent / 100.0);
        String bar = "\u2588".repeat(filled) + "\u2591".repeat(barLength - filled);

        sb.append("\n[ УСПЕШНОСТЬ ]\n");
        sb.append("  ").append(bar).append(" ").append(percent).append("%")
                .append(" (").append(success).append(" из ").append(total).append(")\n");
    }

    private void appendAverageThreat(StringBuilder sb, List<Mission> missions) {
        List<Integer> levels = missions.stream()
                .filter(m -> m.getCurse() != null && m.getCurse().getThreatLevel() != null)
                .map(m -> threatToNumber(m.getCurse().getThreatLevel()))
                .filter(n -> n > 0)
                .toList();

        if (levels.isEmpty()) return;

        double avg = levels.stream().mapToInt(Integer::intValue).average().orElse(0);
        sb.append("\n[ СРЕДНИЙ УРОВЕНЬ УГРОЗЫ ]\n");
        sb.append("  Значение: ").append(String.format("%.1f", avg))
                .append(" (LOW=1, MEDIUM=2, HIGH=3, SPECIAL_GRADE=4)\n");
    }

    private void appendTopSorcerers(StringBuilder sb, List<Mission> missions) {
        // считаем в скольких миссиях участвует каждый колдун
        Map<String, Long> counts = missions.stream()
                .flatMap(m -> m.getSorcerers().stream())
                .map(Sorcerer::getName)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(n -> n, Collectors.counting()));

        if (counts.isEmpty()) return;

        List<Map.Entry<String, Long>> top = counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(3)
                .toList();

        sb.append("\n[ ТОП УЧАСТНИКОВ ]\n");
        int rank = 1;
        for (Map.Entry<String, Long> entry : top) {
            sb.append("  ").append(rank++).append(". ").append(entry.getKey())
                    .append(" \u2014 ").append(entry.getValue())
                    .append(missionWord(entry.getValue())).append("\n");
        }
    }

    private int threatToNumber(String threat) {
        return switch (threat) {
            case "LOW" -> 1;
            case "MEDIUM" -> 2;
            case "HIGH" -> 3;
            case "SPECIAL_GRADE" -> 4;
            default -> 0;
        };
    }

    // склонение слова «миссия» по числу
    private String missionWord(long count) {
        long mod100 = count % 100;
        long mod10 = count % 10;
        if (mod100 >= 11 && mod100 <= 19) return " миссий";
        if (mod10 == 1) return " миссия";
        if (mod10 >= 2 && mod10 <= 4) return " миссии";
        return " миссий";
    }
}
