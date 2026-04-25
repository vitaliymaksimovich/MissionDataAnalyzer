package parser.util;

/**
 * Утилита для безопасного парсинга чисел — один источник правды на весь проект.
 * Раньше эта логика копировалась в 5 местах (TxtMissionParser, PipeMissionParser,
 * CivilianImpactPipeHandler, BlockDataAccessor).
 *
 * При невалидной строке возвращает 0 — вызывающий код ожидает значение по умолчанию,
 * а не исключение (в данных миссий пропуск числа допустим).
 */
public final class NumberParser {

    private NumberParser() {}

    /** Безопасный парсинг double: при ошибке возвращает 0.0 */
    public static double parseDoubleOrZero(String v) {
        if (v == null || v.isBlank()) return 0.0;
        try {
            return Double.parseDouble(v.trim());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    /** Безопасный парсинг int: при ошибке возвращает 0 */
    public static int parseIntOrZero(String v) {
        if (v == null || v.isBlank()) return 0;
        try {
            return Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
