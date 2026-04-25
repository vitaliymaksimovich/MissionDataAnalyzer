package service.filter;

import model.Mission;
import model.Sorcerer;

/**
 * Паттерн: Chain of Responsibility — конкретный обработчик.
 * Критерий: среди участников миссии есть заклинатель с именем, содержащим заданную строку.
 */
public class SorcererFilter implements MissionFilter {

    private final String name;

    public SorcererFilter(String name) {
        this.name = name;
    }

    @Override
    public boolean test(Mission mission) {
        if (mission.getSorcerers() == null || mission.getSorcerers().isEmpty()) {
            return false;
        }
        String query = name.toLowerCase();
        return mission.getSorcerers().stream()
                .map(Sorcerer::getName)
                .filter(n -> n != null)
                .anyMatch(n -> n.toLowerCase().contains(query));
    }

    @Override
    public String getDisplayName() { return "Участник: " + name; }

    @Override
    public String getFilterId() { return "sorcerer"; }

    @Override
    public String getValue() { return name; }
}
