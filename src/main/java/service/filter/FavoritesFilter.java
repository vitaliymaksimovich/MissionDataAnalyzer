package service.filter;

import model.Mission;

import java.util.Set;

/**
 * Паттерн: Chain of Responsibility — конкретный обработчик.
 * Критерий: миссия добавлена в избранное пользователем.
 */
public class FavoritesFilter implements MissionFilter {

    private final Set<Mission> favorites;

    public FavoritesFilter(Set<Mission> favorites) {
        this.favorites = favorites;
    }

    @Override
    public boolean test(Mission mission) {
        return favorites.contains(mission);
    }

    @Override
    public String getDisplayName() { return "Только избранные"; }

    @Override
    public String getFilterId() { return "favorites"; }

    @Override
    public String getValue() { return "true"; }
}
