package service;

import model.*;

import java.util.List;

public class FullReportFormatter implements ReportFormatter {

    @Override
    public String getDisplayName() { return "Полный отчёт"; }

    @Override
    public String format(Mission mission) {
        if (mission == null) return "Данные миссии недоступны.";
        StringBuilder sb = new StringBuilder();

        section(sb, "ОТЧЁТ О МИССИИ");
        field(sb, "ID миссии",       mission.getMissionId());
        field(sb, "Дата",            mission.getDate());
        field(sb, "Локация",         mission.getLocation());
        field(sb, "Результат",       mission.getOutcome());
        field(sb, "Ущерб",           mission.getDamageCost() > 0 ? String.valueOf(mission.getDamageCost()) : null);

        section(sb, "ПРОКЛЯТИЕ");
        appendCurse(sb, mission.getCurse());

        appendIfNotEmpty(sb, "УЧАСТНИКИ", mission.getSorcerers(),
                s -> "  - " + s.getName() + " [" + s.getRank() + "]");

        appendIfNotEmpty(sb, "ТЕХНИКИ", mission.getTechniques(),
                t -> "  - " + t.getName() + " (" + t.getType() + ") | " + t.getOwner() + " | урон: " + t.getDamage());

        appendEconomic(sb, mission.getEconomicAssessment());
        appendCivilian(sb, mission.getCivilianImpact());
        appendEnemy(sb, mission.getEnemyActivity());
        appendEnvironment(sb, mission.getEnvironmentConditions());

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

        return sb.toString();
    }

    private void appendCurse(StringBuilder sb, Curse curse) {
        if (curse == null) {
            sb.append("  Информация о проклятии отсутствует.\n");
            return;
        }
        field(sb, "Название",      curse.getName());
        field(sb, "Уровень угрозы", curse.getThreatLevel());
    }

    private void appendEconomic(StringBuilder sb, EconomicAssessment ea) {
        if (ea == null) return;
        section(sb, "ЭКОНОМИЧЕСКАЯ ОЦЕНКА");
        field(sb, "Общий ущерб",          fmt(ea.getTotalDamageCost()));
        field(sb, "Инфраструктура",        fmt(ea.getInfrastructureDamage()));
        field(sb, "Коммерческий ущерб",    fmt(ea.getCommercialDamage()));
        field(sb, "Транспорт",             fmt(ea.getTransportDamage()));
        field(sb, "Дней на восстановление", String.valueOf(ea.getRecoveryEstimateDays()));
        field(sb, "Страховка",             ea.isInsuranceCovered() ? "Да" : "Нет");
    }

    private void appendCivilian(StringBuilder sb, CivilianImpact ci) {
        if (ci == null) return;
        section(sb, "ГРАЖДАНСКИЕ");
        field(sb, "Эвакуировано",    String.valueOf(ci.getEvacuated()));
        field(sb, "Пострадавших",    String.valueOf(ci.getInjured()));
        field(sb, "Пропавших",       String.valueOf(ci.getMissing()));
        if (ci.getPublicExposureRisk() != null)
            field(sb, "Риск раскрытия", ci.getPublicExposureRisk());
    }

    private void appendEnemy(StringBuilder sb, EnemyActivity ea) {
        if (ea == null) return;
        section(sb, "АКТИВНОСТЬ ПРОТИВНИКА");
        field(sb, "Поведение",      ea.getBehaviorType());
        field(sb, "Мобильность",    ea.getMobility());
        field(sb, "Риск эскалации", ea.getEscalationRisk());

        if (!ea.getTargetPriority().isEmpty()) {
            sb.append("  Приоритеты целей:\n");
            ea.getTargetPriority().forEach(p -> sb.append("    - ").append(p).append("\n"));
        }
        if (!ea.getAttackPatterns().isEmpty()) {
            sb.append("  Паттерны атак:\n");
            ea.getAttackPatterns().forEach(p -> sb.append("    - ").append(p).append("\n"));
        }
        if (!ea.getCountermeasuresUsed().isEmpty()) {
            sb.append("  Контрмеры:\n");
            ea.getCountermeasuresUsed().forEach(p -> sb.append("    - ").append(p).append("\n"));
        }
    }

    private void appendEnvironment(StringBuilder sb, EnvironmentConditions ec) {
        if (ec == null) return;
        section(sb, "УСЛОВИЯ СРЕДЫ");
        field(sb, "Погода",              ec.getWeather());
        field(sb, "Время суток",         ec.getTimeOfDay());
        field(sb, "Видимость",           ec.getVisibility());
        if (ec.getCursedEnergyDensity() > 0)
            field(sb, "Плотность энергии", fmt(ec.getCursedEnergyDensity()));
    }

    // --- утилиты ---

    private void section(StringBuilder sb, String title) {
        sb.append("\n=== ").append(title).append(" ===\n");
    }

    private void field(StringBuilder sb, String label, String value) {
        if (value == null || value.isBlank()) {
            sb.append("  ").append(label).append(": [не указано]\n");
        } else {
            sb.append("  ").append(label).append(": ").append(value).append("\n");
        }
    }

    private <T> void appendIfNotEmpty(StringBuilder sb, String title, List<T> list, java.util.function.Function<T, String> mapper) {
        if (list == null || list.isEmpty()) return;
        section(sb, title);
        list.forEach(item -> sb.append(mapper.apply(item)).append("\n"));
    }

    private void appendStringList(StringBuilder sb, String title, List<String> list) {
        if (list == null || list.isEmpty()) return;
        section(sb, title);
        list.forEach(item -> sb.append("  - ").append(item).append("\n"));
    }

    private String fmt(double v) {
        return v > 0 ? String.valueOf(v) : null;
    }
}