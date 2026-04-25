package service;

import model.Mission;
import parser.FormatDetector;
import parser.MissionParser;
import parser.ParserRegistry;
import parser.ParserRegistryConfig;
import parser.decorator.LoggingParserDecorator;
import parser.decorator.ValidatingParserDecorator;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.function.UnaryOperator;

public class MissionService {

    /** Реестр не final — пересоздаётся в reloadPlugins() при горячей перезагрузке */
    private ParserRegistry registry;

    /**
     * Цепочка декораторов, применяемых к парсеру.
     * Конфигурируется извне — для добавления нового декоратора достаточно
     * добавить элемент в список (OCP: открыт для расширения, закрыт для изменения).
     */
    private final List<UnaryOperator<MissionParser>> decorators;

    public MissionService() {
        this(defaultDecorators());
    }

    public MissionService(List<UnaryOperator<MissionParser>> decorators) {
        this.registry = ParserRegistryConfig.createDefault();
        this.decorators = decorators;
    }

    /** Стандартная цепочка декораторов: валидация → логирование */
    private static List<UnaryOperator<MissionParser>> defaultDecorators() {
        return List.of(
                ValidatingParserDecorator::new,
                LoggingParserDecorator::new
        );
    }

    /** Загрузка миссии из файла (основной сценарий) */
    public Mission loadMission(File file) throws IOException {
        MissionParser parser = registry.getParser(file);
        return applyDecorators(parser).parse(file);
    }

    /** Загрузка миссии из потока данных (REST API, БД) */
    public Mission loadMission(InputStream in, String format) throws IOException {
        MissionParser parser = registry.getParser(format);
        return applyDecorators(parser).parse(in);
    }

    /** Последовательное применение всех декораторов к парсеру */
    private MissionParser applyDecorators(MissionParser parser) {
        MissionParser result = parser;
        for (UnaryOperator<MissionParser> decorator : decorators) {
            result = decorator.apply(result);
        }
        return result;
    }

    /**
     * Определение формата по содержимому — для REST API / InputStream,
     * когда расширения файла нет (например, при загрузке из URL).
     */
    public String detectFormat(String content) {
        return FormatDetector.detect(content);
    }

    /**
     * Горячая перезагрузка плагинов: пересобирает реестр парсеров,
     * чтобы свежие JAR-плагины из papki parser-plugins/ подхватились без рестарта.
     */
    public void reloadPlugins() {
        PluginManager.getInstance().reload();
        this.registry = ParserRegistryConfig.createDefault();
    }
}