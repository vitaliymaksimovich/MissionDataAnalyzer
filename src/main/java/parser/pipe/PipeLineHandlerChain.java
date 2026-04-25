package parser.pipe;

import model.builder.MissionBuilder;

import java.util.List;

/**
 * Цепочка обработчиков pipe-строк.
 * Делегирует строку первому подходящему обработчику.
 */
public class PipeLineHandlerChain {

    private final List<PipeLineHandler> handlers;

    public PipeLineHandlerChain(List<PipeLineHandler> handlers) {
        this.handlers = handlers;
    }

    /** Возвращает true если строка была обработана */
    public boolean handle(String lineType, String[] parts, MissionBuilder builder) {
        for (PipeLineHandler h : handlers) {
            if (h.accepts(lineType)) {
                h.handle(parts, builder);
                return true;
            }
        }
        return false;
    }

    /** Сбрасывает накопленное состояние всех обработчиков */
    public void flushAll(MissionBuilder builder) {
        for (PipeLineHandler h : handlers) {
            h.flush(builder);
        }
    }

    public static PipeLineHandlerChain createDefault() {
        return new PipeLineHandlerChain(List.of(
                new TimelinePipeHandler(),
                new CivilianImpactPipeHandler(),
                new EnemyActionPipeHandler()
                // новый тип строки = одна строка здесь
        ));
    }
}
