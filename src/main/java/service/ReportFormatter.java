package service;

import model.Mission;

public interface ReportFormatter {
    String format(Mission mission);
    String getDisplayName();
}