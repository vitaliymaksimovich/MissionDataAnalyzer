package service;

import model.CivilianImpact;
import model.Curse;
import model.EconomicAssessment;
import model.EnemyActivity;
import model.EnvironmentConditions;
import model.Mission;

import java.util.List;
import java.util.function.Function;

/**
 * Базовый класс для форматтеров отчётов.
 * Содержит общие утилиты форматирования: секции, поля, списки.
 *
 * Также вынесены общие секции (проклятие, экономика, гражданские, противник, среда),
 * чтобы форматтеры отчётов не дублировали одну и ту же логику.
 */
public abstract class AbstractReportFormatter implements ReportFormatter {

    @Override
    public final String format(Mission mission) {
        if (mission == null) return "Данные миссии недоступны.";
        StringBuilder sb = new StringBuilder();
        buildReport(sb, mission);
        return sb.toString();
    }

    /** Формирует тело отчёта — реализуется в подклассах */
    protected abstract void buildReport(StringBuilder sb, Mission mission);

    // --- утилиты форматирования ---

    protected void section(StringBuilder sb, String title) {
        sb.append("\n=== ").append(title).append(" ===\n");
    }

    protected void field(StringBuilder sb, String label, String value) {
        if (value == null || value.isBlank()) {
            sb.append("  ").append(label).append(": [не указано]\n");
        } else {
            sb.append("  ").append(label).append(": ").append(value).append("\n");
        }
    }

    protected String orMissing(String v) {
        return (v == null || v.isBlank()) ? "[не указано]" : v;
    }

    protected <T> void appendIfNotEmpty(StringBuilder sb, String title, List<T> list, Function<T, String> mapper) {
        if (list == null || list.isEmpty()) return;
        section(sb, title);
        list.forEach(item -> sb.append(mapper.apply(item)).append("\n"));
    }

    protected void appendStringList(StringBuilder sb, String title, List<String> list) {
        if (list == null || list.isEmpty()) return;
        section(sb, title);
        list.forEach(item -> sb.append("  - ").append(item).append("\n"));
    }

    protected String fmt(double v) {
        return v > 0 ? String.valueOf(v) : null;
    }

    // -------------------------------------------------------------------------
    // Общие секции — использовались в двух форматтерах, теперь здесь
    // -------------------------------------------------------------------------

    /** Секция «ПРОКЛЯТИЕ» в едином стиле с field()/section() */
    protected void appendCurseSection(StringBuilder sb, Curse curse) {
        section(sb, "ПРОКЛЯТИЕ");
        if (curse == null) {
            sb.append("  Информация о проклятии отсутствует.\n");
            return;
        }
        field(sb, "Название",       curse.getName());
        field(sb, "Уровень угрозы", curse.getThreatLevel());
    }

    /** Секция «ЭКОНОМИЧЕСКАЯ ОЦЕНКА» (если данные есть) */
    protected void appendEconomicSection(StringBuilder sb, EconomicAssessment ea) {
        if (ea == null) return;
        section(sb, "ЭКОНОМИЧЕСКАЯ ОЦЕНКА");
        field(sb, "Общий ущерб",           fmt(ea.getTotalDamageCost()));
        field(sb, "Инфраструктура",         fmt(ea.getInfrastructureDamage()));
        field(sb, "Коммерческий ущерб",     fmt(ea.getCommercialDamage()));
        field(sb, "Транспорт",              fmt(ea.getTransportDamage()));
        field(sb, "Дней на восстановление", String.valueOf(ea.getRecoveryEstimateDays()));
        field(sb, "Страховка",              ea.isInsuranceCovered() ? "Да" : "Нет");
    }

    /** Секция «ГРАЖДАНСКИЕ» (если данные есть) */
    protected void appendCivilianSection(StringBuilder sb, CivilianImpact ci) {
        if (ci == null) return;
        section(sb, "ГРАЖДАНСКИЕ");
        field(sb, "Эвакуировано",    String.valueOf(ci.getEvacuated()));
        field(sb, "Пострадавших",    String.valueOf(ci.getInjured()));
        field(sb, "Пропавших",       String.valueOf(ci.getMissing()));
        if (ci.getPublicExposureRisk() != null)
            field(sb, "Риск раскрытия", ci.getPublicExposureRisk());
    }

    /** Секция «АКТИВНОСТЬ ПРОТИВНИКА» (если данные есть) */
    protected void appendEnemySection(StringBuilder sb, EnemyActivity ea) {
        if (ea == null) return;
        section(sb, "АКТИВНОСТЬ ПРОТИВНИКА");
        field(sb, "Поведение",      ea.getBehaviorType());
        field(sb, "Мобильность",    ea.getMobility());
        field(sb, "Риск эскалации", ea.getEscalationRisk());

        appendIndentedList(sb, "Приоритеты целей", ea.getTargetPriority());
        appendIndentedList(sb, "Паттерны атак",    ea.getAttackPatterns());
        appendIndentedList(sb, "Контрмеры",        ea.getCountermeasuresUsed());
    }

    /** Секция «УСЛОВИЯ СРЕДЫ» (если данные есть) */
    protected void appendEnvironmentSection(StringBuilder sb, EnvironmentConditions ec) {
        if (ec == null) return;
        section(sb, "УСЛОВИЯ СРЕДЫ");
        field(sb, "Погода",      ec.getWeather());
        field(sb, "Время суток", ec.getTimeOfDay());
        field(sb, "Видимость",   ec.getVisibility());
        if (ec.getCursedEnergyDensity() > 0)
            field(sb, "Плотность энергии", fmt(ec.getCursedEnergyDensity()));
    }

    /** Вспомогательный вывод: заголовок + список с отступом (используется в enemy-секции) */
    private void appendIndentedList(StringBuilder sb, String title, List<String> list) {
        if (list == null || list.isEmpty()) return;
        sb.append("  ").append(title).append(":\n");
        list.forEach(p -> sb.append("    - ").append(p).append("\n"));
    }
}
