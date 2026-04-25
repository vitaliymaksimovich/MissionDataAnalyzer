package service;

import java.util.ArrayList;
import java.util.List;

/**
 * Паттерн: Registry (реестр объектов).
 *
 * Хранит все доступные стратегии формирования отчёта и предоставляет их клиенту.
 * Отделяет создание/регистрацию форматтеров (ReportFormatterRegistryConfig)
 * от их использования (MainFrame).
 *
 * Новый форматтер = одна строка регистрации в ReportFormatterRegistryConfig.
 */
public class ReportFormatterRegistry {

    private final List<ReportFormatter> formatters = new ArrayList<>();

    public void register(ReportFormatter formatter) {
        formatters.add(formatter);
    }

    public List<ReportFormatter> getAll() {
        return List.copyOf(formatters);
    }
}
