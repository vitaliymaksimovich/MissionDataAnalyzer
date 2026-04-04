package service;

import model.Mission;
import parser.MissionParser;
import parser.ParserRegistry;
import parser.ParserRegistryConfig;
import parser.decorator.LoggingParserDecorator;
import parser.decorator.ValidatingParserDecorator;

import java.io.File;
import java.io.IOException;

public class MissionService {

    private final ParserRegistry registry;

    public MissionService() {
        this.registry = ParserRegistryConfig.createDefault();
    }

    public Mission loadMission(File file) throws IOException {
        MissionParser parser = registry.getParser(file);
        MissionParser decorated = new LoggingParserDecorator(
                new ValidatingParserDecorator(parser)
        );
        return decorated.parse(file);
    }
}