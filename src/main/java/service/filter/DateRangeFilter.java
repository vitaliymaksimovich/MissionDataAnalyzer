package service.filter;

import model.Mission;

public class DateRangeFilter implements MissionFilter {

    private final String from; // включительно, формат yyyy-MM-dd
    private final String to;

    public DateRangeFilter(String from, String to) {
        this.from = from;
        this.to = to;
    }

    @Override
    public boolean test(Mission mission) {
        String date = mission.getDate();
        if (date == null || date.isBlank()) return false;
        // сравниваем строки — работает для формата yyyy-MM-dd
        if (from != null && !from.isBlank() && date.compareTo(from) < 0) return false;
        if (to != null && !to.isBlank() && date.compareTo(to) > 0) return false;
        return true;
    }

    @Override
    public String getDisplayName() { return "Дата: " + from + " — " + to; }
}