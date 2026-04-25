package service;

import model.Mission;

public class FullReportFormatter extends AbstractReportFormatter {

    @Override
    public String getDisplayName() { return "Полный отчёт"; }

    @Override
    protected void buildReport(StringBuilder sb, Mission mission) {
        section(sb, "ОТЧЁТ О МИССИИ");
        field(sb, "ID миссии",       mission.getMissionId());
        field(sb, "Дата",            mission.getDate());
        field(sb, "Локация",         mission.getLocation());
        field(sb, "Результат",       mission.getOutcome());
        field(sb, "Ущерб",           mission.getDamageCost() > 0 ? String.valueOf(mission.getDamageCost()) : null);

        // общие секции — реализованы в AbstractReportFormatter
        appendCurseSection(sb, mission.getCurse());

        appendIfNotEmpty(sb, "УЧАСТНИКИ", mission.getSorcerers(),
                s -> "  - " + s.getName() + " [" + s.getRank() + "]");

        appendIfNotEmpty(sb, "ТЕХНИКИ", mission.getTechniques(),
                t -> "  - " + t.getName() + " (" + t.getType() + ") | " + t.getOwner() + " | урон: " + t.getDamage());

        appendEconomicSection(sb, mission.getEconomicAssessment());
        appendCivilianSection(sb, mission.getCivilianImpact());
        appendEnemySection(sb, mission.getEnemyActivity());
        appendEnvironmentSection(sb, mission.getEnvironmentConditions());

        appendIfNotEmpty(sb, "ХРОНОЛОГИЯ", mission.getOperationTimeline(),
                e -> "  [" + e.getTimestamp() + "] " + e.getType() + " — " + e.getDescription());

        appendStringList(sb, "ТЕГИ",                   mission.getOperationTags());
        appendStringList(sb, "ВСПОМОГАТЕЛЬНЫЕ СИЛЫ",   mission.getSupportUnits());
        appendStringList(sb, "РЕКОМЕНДАЦИИ",            mission.getRecommendations());
        appendStringList(sb, "НАЙДЕННЫЕ АРТЕФАКТЫ",     mission.getArtifactsRecovered());
        appendStringList(sb, "ЗОНЫ ЭВАКУАЦИИ",         mission.getEvacuationZones());
        appendStringList(sb, "ЭФФЕКТЫ",                mission.getStatusEffects());

        if (mission.getComment() != null && !mission.getComment().isBlank()) {
            section(sb, "КОММЕНТАРИЙ");
            sb.append("  ").append(mission.getComment()).append("\n");
        }

        appendStringList(sb, "НЕРАСПОЗНАННЫЕ ДАННЫЕ",  mission.getUnparsedData());
    }
}
