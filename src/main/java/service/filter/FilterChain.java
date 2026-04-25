package service.filter;

import model.Mission;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Паттерн: Chain of Responsibility — менеджер цепочки.
 *
 * Хранит список обработчиков (MissionFilter) и последовательно применяет каждый
 * к входному набору миссий. Миссия проходит цепочку, только если все звенья
 * вернули true — это логическое AND нескольких независимых критериев.
 *
 * Цепочка динамически собирается в FilterDialog и передаётся в MissionListPanel.
 * Добавление/удаление фильтра не требует изменения этого класса.
 */
public class FilterChain {

    // Цепочка обработчиков: каждый фильтр — одно звено
    private final List<MissionFilter> filters = new ArrayList<>();

    public void add(MissionFilter filter) { filters.add(filter); }
    public void clear() { filters.clear(); }
    public boolean isEmpty() { return filters.isEmpty(); }
    public List<MissionFilter> getFilters() { return filters; }

    /**
     * Прогоняет список миссий через всю цепочку фильтров.
     * Миссия остаётся в результате только если ВСЕ звенья вернули true.
     */
    public List<Mission> apply(List<Mission> missions) {
        return missions.stream()
                .filter(m -> filters.stream().allMatch(f -> f.test(m)))
                .collect(Collectors.toList());
    }

    // Поиск по тексту применяется поверх цепочки фильтров
    public List<Mission> applyWithSearch(List<Mission> missions, String query) {
        List<Mission> filtered = apply(missions);
        if (query == null || query.isBlank()) return filtered;
        String q = query.toLowerCase();
        return filtered.stream()
                .filter(m -> matches(m, q))
                .collect(Collectors.toList());
    }

    private boolean matches(Mission m, String q) {
        if (m.getMissionId() != null && m.getMissionId().toLowerCase().contains(q)) return true;
        if (m.getLocation() != null && m.getLocation().toLowerCase().contains(q)) return true;
        if (m.getOutcome() != null && m.getOutcome().toLowerCase().contains(q)) return true;
        if (m.getCurse() != null && m.getCurse().getName() != null
                && m.getCurse().getName().toLowerCase().contains(q)) return true;
        return false;
    }
}