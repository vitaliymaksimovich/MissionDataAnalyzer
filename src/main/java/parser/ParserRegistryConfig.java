package parser;

public class ParserRegistryConfig {

    public static ParserRegistry createDefault() {
        ParserRegistry registry = new ParserRegistry();
        registry.register("json",  JsonMissionParser::new);
        registry.register("xml",   XmlMissionParser::new);
        registry.register("txt",   TxtMissionParser::new);
        registry.register("yaml",  YamlMissionParser::new);
        registry.register("yml",   YamlMissionParser::new);
        registry.register("pipe", PipeMissionParser::new);
        // Новый формат = одна строка:
        // registry.register("csv", CsvMissionParser::new);
        return registry;
    }

    private ParserRegistryConfig() {}
}