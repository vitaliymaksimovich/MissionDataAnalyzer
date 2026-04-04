package service.filter;

import model.Mission;

public class ThreatLevelFilter implements MissionFilter {

    private final String threatLevel;

    public ThreatLevelFilter(String threatLevel) {
        this.threatLevel = threatLevel;
    }

    @Override
    public boolean test(Mission mission) {
        if (mission.getCurse() == null) return false;
        return threatLevel.equalsIgnoreCase(mission.getCurse().getThreatLevel());
    }

    @Override
    public String getDisplayName() { return "Уровень угрозы: " + threatLevel; }
}
