package service.filter;

import model.Mission;

public class OutcomeFilter implements MissionFilter {

    private final String outcome;

    public OutcomeFilter(String outcome) {
        this.outcome = outcome;
    }

    @Override
    public boolean test(Mission mission) {
        return outcome.equalsIgnoreCase(mission.getOutcome());
    }

    @Override
    public String getDisplayName() { return "Результат: " + outcome; }
}
