package parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import model.Curse;
import model.Sorcerer;
import model.Technique;
import model.Mission;
import model.builder.MissionBuilder;
import parser.block.BlockParserChain;
import parser.block.util.BlockDataAccessor;

import java.io.File;
import java.io.IOException;
import java.util.Set;

public class JsonMissionParser extends MissionParser {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final Set<String> CORE_FIELDS = Set.of(
            "missionId", "date", "location", "outcome", "damageCost",
            "curse", "sorcerers", "techniques", "comment"
    );

    private final BlockParserChain blockChain;

    public JsonMissionParser() {
        this.blockChain = BlockParserChain.createDefault();
    }

    @Override
    public Mission parse(File file) throws IOException {
        JsonNode root = MAPPER.readTree(file);
        MissionBuilder builder = new MissionBuilder();

        builder.missionId(text(root, "missionId"))
                .date(text(root, "date"))
                .location(text(root, "location"))
                .outcome(text(root, "outcome"))
                .damageCost(root.path("damageCost").asDouble(0))
                .comment(text(root, "comment"));

        parseCurse(root.path("curse"), builder);
        parseSorcerers(root.path("sorcerers"), builder);
        parseTechniques(root.path("techniques"), builder);

        // все неизвестные блоки — в цепочку или в unparsed
        root.fieldNames().forEachRemaining(field -> {
            if (!CORE_FIELDS.contains(field)) {
                if (!blockChain.handle(field, root.get(field), builder)) {
                    builder.addUnparsed(field + ": " + root.get(field));
                }
            }
        });

        return builder.build();
    }

    private void parseCurse(JsonNode node, MissionBuilder builder) {
        if (node.isMissingNode()) return;
        builder.curse(new Curse(
                node.path("name").asText(null),
                node.path("threatLevel").asText(null)
        ));
    }

    private void parseSorcerers(JsonNode node, MissionBuilder builder) {
        if (!node.isArray()) return;
        node.forEach(n -> builder.addSorcerer(new Sorcerer(
                n.path("name").asText(null),
                n.path("rank").asText(null)
        )));
    }

    private void parseTechniques(JsonNode node, MissionBuilder builder) {
        if (!node.isArray()) return;
        node.forEach(n -> builder.addTechnique(new Technique(
                n.path("name").asText(null),
                n.path("type").asText(null),
                n.path("owner").asText(null),
                n.path("damage").asDouble(0)
        )));
    }

    private String text(JsonNode root, String field) {
        JsonNode n = root.path(field);
        return n.isMissingNode() || n.isNull() ? null : n.asText();
    }
}