package service.filter;

import model.Mission;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class FilterChain {

    private final List<MissionFilter> filters = new ArrayList<>();

    public void add(MissionFilter filter) { filters.add(filter); }
    public void clear() { filters.clear(); }
    public boolean isEmpty() { return filters.isEmpty(); }
    public List<MissionFilter> getFilters() { return filters; }

    public List<Mission> apply(List<Mission> missions) {
        return missions.stream()
                .filter(m -> filters.stream().allMatch(f -> f.test(m)))
                .collect(Collectors.toList());
    }

    // поиск по тексту — по id, локации, названию проклятия
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