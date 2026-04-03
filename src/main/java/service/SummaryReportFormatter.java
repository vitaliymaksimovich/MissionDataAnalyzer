package service;

import model.Mission;

public class SummaryReportFormatter implements ReportFormatter {

    @Override
    public String getDisplayName() { return "Краткое резюме"; }

    @Override
    public String format(Mission mission) {
        if (mission == null) return "Данные миссии недоступны.";
        StringBuilder sb = new StringBuilder();

        sb.append("=== КРАТКОЕ РЕЗЮМЕ ===\n");
        sb.append("  Миссия:   ").append(orMissing(mission.getMissionId())).append("\n");
        sb.append("  Дата:     ").append(orMissing(mission.getDate())).append("\n");
        sb.append("  Локация:  ").append(orMissing(mission.getLocation())).append("\n");
        sb.append("  Итог:     ").append(orMissing(mission.getOutcome())).append("\n");

        if (mission.getCurse() != null) {
            sb.append("  Цель:     ").append(orMissing(mission.getCurse().getName()))
                    .append(" [").append(orMissing(mission.getCurse().getThreatLevel())).append("]\n");
        } else {
            sb.append("  Цель:     [не указано]\n");
        }

        if (!mission.getSorcerers().isEmpty()) {
            sb.append("  Маги:     ");
            mission.getSorcerers().forEach(s -> sb.append(s.getName()).append(" "));
            sb.append("\n");
        }

        if (mission.getDamageCost() > 0)
            sb.append("  Ущерб:    ").append(mission.getDamageCost()).append("\n");

        return sb.toString();
    }

    private String orMissing(String v) {
        return (v == null || v.isBlank()) ? "[не указано]" : v;
    }
}