package service;

import model.Mission;

public class RiskReportFormatter extends AbstractReportFormatter {

    @Override
    public String getDisplayName() { return "Отчёт по рискам"; }

    @Override
    protected void buildReport(StringBuilder sb, Mission mission) {
        sb.append("=== ОТЧЁТ ПО РИСКАМ ===\n");
        sb.append("  Миссия:  ").append(orMissing(mission.getMissionId())).append("\n");
        sb.append("  Итог:    ").append(orMissing(mission.getOutcome())).append("\n\n");

        sb.append("[ УГРОЗА ]\n");
        if (mission.getCurse() != null) {
            sb.append("  Проклятие:    ").append(orMissing(mission.getCurse().getName())).append("\n");
            sb.append("  Уровень:      ").append(orMissing(mission.getCurse().getThreatLevel())).append("\n");
        } else {
            sb.append("  Проклятие: [не указано]\n");
        }

        if (mission.getEnemyActivity() != null) {
            var ea = mission.getEnemyActivity();
            sb.append("  Поведение:    ").append(orMissing(ea.getBehaviorType())).append("\n");
            sb.append("  Эскалация:    ").append(orMissing(ea.getEscalationRisk())).append("\n");
            sb.append("  Мобильность:  ").append(orMissing(ea.getMobility())).append("\n");
            if (!ea.getAttackPatterns().isEmpty()) {
                sb.append("  Паттерны атак:\n");
                ea.getAttackPatterns().forEach(p -> sb.append("    - ").append(p).append("\n"));
            }
        }

        sb.append("\n[ УЩЕРБ ]\n");
        sb.append("  Общий ущерб: ").append(mission.getDamageCost() > 0 ? mission.getDamageCost() : "[не указано]").append("\n");
        if (mission.getEconomicAssessment() != null) {
            var ec = mission.getEconomicAssessment();
            if (ec.getInfrastructureDamage() > 0) sb.append("  Инфраструктура: ").append(ec.getInfrastructureDamage()).append("\n");
            if (ec.getCommercialDamage() > 0)     sb.append("  Коммерческий:   ").append(ec.getCommercialDamage()).append("\n");
            if (ec.getTransportDamage() > 0)       sb.append("  Транспорт:      ").append(ec.getTransportDamage()).append("\n");
            if (ec.getRecoveryEstimateDays() > 0)  sb.append("  Восстановление: ").append(ec.getRecoveryEstimateDays()).append(" дней\n");
        }

        if (mission.getCivilianImpact() != null) {
            sb.append("\n[ ГРАЖДАНСКИЕ ]\n");
            var ci = mission.getCivilianImpact();
            sb.append("  Эвакуировано: ").append(ci.getEvacuated()).append("\n");
            sb.append("  Пострадавших: ").append(ci.getInjured()).append("\n");
            sb.append("  Пропавших:    ").append(ci.getMissing()).append("\n");
        }

        if (mission.getEnvironmentConditions() != null) {
            var env = mission.getEnvironmentConditions();
            sb.append("\n[ СРЕДА ]\n");
            sb.append("  Погода:           ").append(orMissing(env.getWeather())).append("\n");
            sb.append("  Время суток:      ").append(orMissing(env.getTimeOfDay())).append("\n");
            sb.append("  Видимость:        ").append(orMissing(env.getVisibility())).append("\n");
            if (env.getCursedEnergyDensity() > 0)
                sb.append("  Плотность энергии: ").append(env.getCursedEnergyDensity()).append("\n");
        }
    }
}
