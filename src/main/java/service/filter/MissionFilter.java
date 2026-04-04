package service.filter;

import model.Mission;

public interface MissionFilter {
    boolean test(Mission mission);
    String getDisplayName();
}