package parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import model.Curse;
import model.Mission;
import model.Sorcerer;
import model.Technique;
import model.builder.MissionBuilder;
import parser.block.BlockParserChain;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class YamlMissionParser extends MissionParser {

    private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory());

    private static final Set<String> CORE_FIELDS = Set.of(
            "missionId", "date", "location", "outcome", "damageCost",
            "curse", "sorcerers", "techniques", "comment"
    );

    private final BlockParserChain blockChain;

    public YamlMissionParser() {
        this.blockChain = BlockParserChain.createDefault();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Mission parse(File file) throws IOException {
        validate(file);
        Map<String, Object> root = MAPPER.readValue(file, Map.class);
        MissionBuilder builder = new MissionBuilder();

        builder.missionId(str(root, "missionId"))
                .date(str(root, "date"))
                .location(str(root, "location"))
                .outcome(str(root, "outcome"))
                .damageCost(dbl(root, "damageCost"))
                .comment(str(root, "comment"));

        parseCurse(root, builder);
        parseSorcerers(root, builder);
        parseTechniques(root, builder);

        for (Map.Entry<String, Object> entry : root.entrySet()) {
            if (!CORE_FIELDS.contains(entry.getKey())) {
                if (!blockChain.handle(entry.getKey(), entry.getValue(), builder)) {
                    builder.addUnparsed(entry.getKey() + ": " + entry.getValue());
                }
            }
        }

        return builder.build();
    }

    @SuppressWarnings("unchecked")
    private void parseCurse(Map<String, Object> root, MissionBuilder builder) {
        Object curse = root.get("curse");
        if (!(curse instanceof Map<?, ?> map)) return;
        builder.curse(new Curse(
                str((Map<String, Object>) map, "name"),
                str((Map<String, Object>) map, "threatLevel")
        ));
    }

    @SuppressWarnings("unchecked")
    private void parseSorcerers(Map<String, Object> root, MissionBuilder builder) {
        Object list = root.get("sorcerers");
        if (!(list instanceof List<?> sorcerers)) return;
        for (Object item : sorcerers) {
            if (item instanceof Map<?, ?> map) {
                Map<String, Object> m = (Map<String, Object>) map;
                builder.addSorcerer(new Sorcerer(str(m, "name"), str(m, "rank")));
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void parseTechniques(Map<String, Object> root, MissionBuilder builder) {
        Object list = root.get("techniques");
        if (!(list instanceof List<?> techniques)) return;
        for (Object item : techniques) {
            if (item instanceof Map<?, ?> map) {
                Map<String, Object> m = (Map<String, Object>) map;
                builder.addTechnique(new Technique(
                        str(m, "name"), str(m, "type"),
                        str(m, "owner"), dbl(m, "damage")
                ));
            }
        }
    }

    private String str(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v != null ? v.toString() : null;
    }

    private double dbl(Map<String, Object> map, String key) {
        Object v = map.get(key);
        if (v == null) return 0;
        try { return Double.parseDouble(v.toString()); } catch (NumberFormatException e) { return 0; }
    }
}