package service;

import model.Curse;
import model.Mission;
import model.Sorcerer;
import model.Technique;

import java.util.List;

public class MissionReportFormatter {

    public String format(Mission mission) {
        if (mission == null) {
            return "Данные миссии недоступны.";
        }

        StringBuilder sb = new StringBuilder();

        sb.append("=== ОТЧЁТ О МИССИИ ===\n");
        sb.append("ID миссии: ").append(safe(mission.getMissionId())).append("\n");
        sb.append("Дата: ").append(safe(mission.getDate())).append("\n");
        sb.append("Локация: ").append(safe(mission.getLocation())).append("\n");
        sb.append("Результат: ").append(safe(mission.getOutcome())).append("\n");
        sb.append("Стоимость ущерба: ").append(mission.getDamageCost()).append("\n\n");

        sb.append("=== ПРОКЛЯТИЕ ===\n");
        appendCurse(sb, mission.getCurse());

        sb.append("\n=== УЧАСТНИКИ МИССИИ ===\n");
        appendSorcerers(sb, mission);

        sb.append("\n=== ИСПОЛЬЗОВАННЫЕ ТЕХНИКИ ===\n");
        appendTechniques(sb, mission);

        sb.append("\n=== КОММЕНТАРИЙ ===\n");
        appendComment(sb, mission.getComment());

        sb.append("\n=== НЕОБРАБОТАННЫЕ ДАННЫЕ ===\n");
        appendUnparsedData(sb, mission.getUnparsedData());

        return sb.toString();
    }

    private void appendCurse(StringBuilder sb, Curse curse) {
        if (curse == null) {
            sb.append("Информация о проклятии отсутствует.\n");
            return;
        }

        sb.append("Название: ").append(safe(curse.getName())).append("\n");
        sb.append("Уровень угрозы: ").append(safe(curse.getThreatLevel())).append("\n");
    }

    private void appendSorcerers(StringBuilder sb, Mission mission) {
        if (mission.getSorcerers() == null || mission.getSorcerers().isEmpty()) {
            sb.append("Нет данных об участниках.\n");
            return;
        }

        for (Sorcerer sorcerer : mission.getSorcerers()) {
            sb.append("- Имя: ").append(safe(sorcerer.getName()))
                    .append(", Ранг: ").append(safe(sorcerer.getRank()))
                    .append("\n");
        }
    }

    private void appendTechniques(StringBuilder sb, Mission mission) {
        if (mission.getTechniques() == null || mission.getTechniques().isEmpty()) {
            sb.append("Нет данных о техниках.\n");
            return;
        }

        for (Technique technique : mission.getTechniques()) {
            sb.append("- Название: ").append(safe(technique.getName()))
                    .append(", Тип: ").append(safe(technique.getType()))
                    .append(", Владелец: ").append(safe(technique.getOwner()))
                    .append(", Урон: ").append(technique.getDamage())
                    .append("\n");
        }
    }

    private void appendComment(StringBuilder sb, String comment) {
        if (comment == null || comment.isBlank()) {
            sb.append("Комментарий отсутствует.\n");
            return;
        }

        sb.append(comment).append("\n");
    }

    private void appendUnparsedData(StringBuilder sb, List<String> unparsedData) {
        if (unparsedData == null || unparsedData.isEmpty()) {
            sb.append("Все данные успешно обработаны.\n");
            return;
        }

        sb.append("Этот текст обработать не вышло:\n");
        for (String line : unparsedData) {
            sb.append("- ").append(line).append("\n");
        }
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "N/A" : value;
    }
}