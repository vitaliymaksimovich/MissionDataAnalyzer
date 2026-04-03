package service;

import model.Mission;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BatchStatsFormatter {

    public String format(List<Mission> missions) {
        if (missions == null || missions.isEmpty()) return "Нет загруженных миссий.";

        StringBuilder sb = new StringBuilder();
        sb.append("=== СТАТИСТИКА ПО МИССИЯМ ===\n\n");
        sb.append("  Всего миссий: ").append(missions.size()).append("\n");

        // итоги
        Map<String, Long> outcomes = missions.stream()
                .collect(Collectors.groupingBy(
                        m -> m.getOutcome() != null ? m.getOutcome() : "UNKNOWN",
                        Collectors.counting()
                ));
        sb.append("\n[ ИТОГИ ]\n");
        outcomes.forEach((k, v) -> sb.append("  ").append(k).append(": ").append(v).append("\n"));

        // общий ущерб
        double totalDamage = missions.stream().mapToDouble(Mission::getDamageCost).sum();
        sb.append("\n[ УЩЕРБ ]\n");
        sb.append("  Суммарный ущерб: ").append(totalDamage).append("\n");
        missions.stream()
                .max((a, b) -> Double.compare(a.getDamageCost(), b.getDamageCost()))
                .ifPresent(m -> sb.append("  Самая дорогая: ").append(m.getMissionId())
                        .append(" (").append(m.getDamageCost()).append(")\n"));

        // уровни угроз
        Map<String, Long> threats = missions.stream()
                .filter(m -> m.getCurse() != null && m.getCurse().getThreatLevel() != null)
                .collect(Collectors.groupingBy(
                        m -> m.getCurse().getThreatLevel(),
                        Collectors.counting()
                ));
        sb.append("\n[ УРОВНИ УГРОЗ ]\n");
        threats.forEach((k, v) -> sb.append("  ").append(k).append(": ").append(v).append("\n"));

        // топ локаций
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
}