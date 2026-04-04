package parser.block;

import model.builder.MissionBuilder;

public interface MissionBlockParser {
    /**
     * Возвращает true если блок был распознан и обработан.
     * Данные передаются в виде Object — каждый формат кладёт то, что умеет:
     * JSON → JsonNode, XML → org.w3c.dom.Node, TXT/YAML → Map<String,Object>
     */
    boolean accepts(String blockName);
    void parse(Object blockData, MissionBuilder builder);
}
