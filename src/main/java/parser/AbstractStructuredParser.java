package parser;

import model.DomainFactory;
import model.Mission;
import model.builder.MissionBuilder;
import parser.block.BlockParserChain;
import parser.block.util.BlockDataAccessor;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Template Method для структурированных форматов (JSON, XML, YAML).
 * Алгоритм: прочитать данные → core fields → curse/sorcerers/techniques → цепочка блоков → unparsed.
 * Подклассу остаётся только реализовать readRootData().
 */
public abstract class AbstractStructuredParser extends MissionParser {

    private final BlockParserChain blockChain = BlockParserChain.createDefault();

    /** Читает файл и возвращает корневой объект (JsonNode / Map / Element) */
    protected abstract Object readRootData(File file) throws IOException;

    @Override
    public Mission parse(File file) throws IOException {
        Object rawRoot = readRootData(file);
        BlockDataAccessor root = new BlockDataAccessor(rawRoot);
        MissionBuilder builder = new MissionBuilder();

        // Основные поля
        builder.missionId(root.getString("missionId"))
                .date(root.getString("date"))
                .location(root.getString("location"))
                .outcome(root.getString("outcome"))
                .damageCost(root.getDouble("damageCost"))
                .comment(root.getString("comment"));

        // Проклятие
        BlockDataAccessor curseData = root.getAccessor("curse");
        if (curseData != null) {
            builder.curse(DomainFactory.createCurse(curseData));
        }

        // Участники
        List<BlockDataAccessor> sorcerers = root.getAccessorList("sorcerers");
        sorcerers.forEach(s -> builder.addSorcerer(DomainFactory.createSorcerer(s)));

        // Техники
        List<BlockDataAccessor> techniques = root.getAccessorList("techniques");
        techniques.forEach(t -> builder.addTechnique(DomainFactory.createTechnique(t)));

        // Остальные блоки — делегируем в цепочку или в unparsed
        for (String field : root.getFieldNames()) {
            if (!CORE_FIELDS.contains(field)) {
                Object rawChild = root.getRawChild(field);
                if (rawChild != null && !blockChain.handle(field, rawChild, builder)) {
                    builder.addUnparsed(field + ": " + rawChild);
                }
            }
        }

        return builder.build();
    }
}
