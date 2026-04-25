package parser.pipe;

import model.builder.MissionBuilder;

/**
 * Обработчик одного типа строки pipe-формата.
 * Аналог MissionBlockParser, но для построчного pipe-парсинга.
 */
public interface PipeLineHandler {

    /** Возвращает true если этот обработчик принимает данный тип строки */
    boolean accepts(String lineType);

    /** Обрабатывает одну строку (может вызываться несколько раз для одного типа) */
    void handle(String[] parts, MissionBuilder builder);

    /** Вызывается после обработки всех строк — для сброса накопленного состояния */
    default void flush(MissionBuilder builder) {}
}
