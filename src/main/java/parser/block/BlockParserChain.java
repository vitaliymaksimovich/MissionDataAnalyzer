package parser.block;

import model.builder.MissionBuilder;

import java.util.List;

public class BlockParserChain {

    private final List<MissionBlockParser> parsers;

    public BlockParserChain(List<MissionBlockParser> parsers) {
        this.parsers = parsers;
    }

    /**
     * Пытается обработать блок. Возвращает true если кто-то из цепочки принял его.
     */
    public boolean handle(String blockName, Object blockData, MissionBuilder builder) {
        for (MissionBlockParser p : parsers) {
            if (p.accepts(blockName)) {
                p.parse(blockData, builder);
                return true;
            }
        }
        return false;
    }

    public static BlockParserChain createDefault() {
        return new BlockParserChain(List.of(
                new EconomicAssessmentBlockParser(),
                new CivilianImpactBlockParser(),
                new EnemyActivityBlockParser(),
                new EnvironmentBlockParser(),
                new TimelineBlockParser()
                // новый блок = одна строка здесь
        ));
    }
}
