package service;

/**
 * Паттерн: Registry + OCP (Open/Closed Principle).
 *
 * Конфигурация реестра форматтеров: регистрирует встроенные стратегии и
 * подключает плагины из parser-plugins/ без изменения этого класса.
 *
 * Расширение системы:
 *  - встроенный форматтер → добавить registry.register(new MyFormatter())
 *  - плагин → положить JAR в parser-plugins/ (этот файл не меняется)
 */
public class ReportFormatterRegistryConfig {

    public static ReportFormatterRegistry createDefault() {
        ReportFormatterRegistry registry = new ReportFormatterRegistry();

        // Встроенные стратегии — зарегистрированы явно (Strategy: три разных алгоритма)
        registry.register(new FullReportFormatter());
        registry.register(new SummaryReportFormatter());
        registry.register(new RiskReportFormatter());

        // OCP: плагины из parser-plugins/ появляются в комбобоксе без перекомпиляции ядра
        PluginManager pm = PluginManager.getInstance();
        pm.loadIfNeeded();
        pm.getActiveReportPlugins().forEach(registry::register);

        return registry;
    }
}
