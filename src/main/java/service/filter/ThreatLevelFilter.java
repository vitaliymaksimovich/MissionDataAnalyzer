package service.filter;

import model.Mission;

/**
 * Паттерн: Chain of Responsibility — конкретный обработчик.
 * Критерий: уровень угрозы проклятия миссии должен совпадать с заданным.
 */
public class ThreatLevelFilter implements MissionFilter {

    private final String threatLevel;

    public ThreatLevelFilter(String threatLevel) {
        this.threatLevel = threatLevel;
    }

    @Override
    public boolean test(Mission mission) {
        // Null-safe: нет миссии, проклятия или уровня угрозы — не подходит
        if (mission == null || mission.getCurse() == null
                || mission.getCurse().getThreatLevel() == null) return false;
        return threatLevel.equalsIgnoreCase(mission.getCurse().getThreatLevel());
    }

    @Override
    public String getDisplayName() { return "Уровень угрозы: " + threatLevel; }

    @Override
    public String getFilterId() { return "threatLevel"; }

    @Override
    public String getValue() { return threatLevel; }
}
