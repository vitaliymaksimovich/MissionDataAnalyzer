package service.filter;

import model.Mission;

/**
 * Паттерн: Chain of Responsibility — конкретный обработчик.
 * Критерий: результат миссии (SUCCESS / FAILURE / PARTIAL_SUCCESS).
 */
public class OutcomeFilter implements MissionFilter {

    private final String outcome;

    public OutcomeFilter(String outcome) {
        this.outcome = outcome;
    }

    @Override
    public boolean test(Mission mission) {
        // Null-safe: если у миссии нет результата, она не подходит под фильтр
        if (mission == null || mission.getOutcome() == null) return false;
        return outcome.equalsIgnoreCase(mission.getOutcome());
    }

    @Override
    public String getDisplayName() { return "Результат: " + outcome; }

    @Override
    public String getFilterId() { return "outcome"; }

    @Override
    public String getValue() { return outcome; }
}
