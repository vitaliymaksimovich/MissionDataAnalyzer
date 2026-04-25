package parser;

import model.Curse;
import model.Mission;
import model.Sorcerer;
import model.Technique;
import model.builder.MissionBuilder;
import parser.pipe.PipeLineHandlerChain;

import java.io.File;
import java.io.IOException;

public class PipeMissionParser extends MissionParser {

    private final PipeLineHandlerChain lineChain;

    public PipeMissionParser() {
        this.lineChain = PipeLineHandlerChain.createDefault();
    }

    @Override
    public Mission parse(File file) throws IOException {
        String[] lines = readFile(file).split("\\R");
        MissionBuilder builder = new MissionBuilder();

        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty()) continue;

            String[] parts = line.split("\\|");
            String type = parts[0];

            switch (type) {
                case "MISSION_CREATED" -> builder
                        .missionId(get(parts, 1))
                        .date(get(parts, 2))
                        .location(get(parts, 3));

                case "CURSE_DETECTED" -> builder.curse(
                        new Curse(get(parts, 1), get(parts, 2))
                );

                case "SORCERER_ASSIGNED" -> builder.addSorcerer(
                        new Sorcerer(get(parts, 1), get(parts, 2))
                );

                case "TECHNIQUE_USED" -> builder.addTechnique(
                        new Technique(
                                get(parts, 1), get(parts, 2),
                                get(parts, 3), parseDouble(get(parts, 4))
                        )
                );

                case "MISSION_RESULT" -> builder
                        .outcome(get(parts, 1))
                        .damageCost(parseDouble(extractValue(get(parts, 2))));

                default -> {
                    // все остальные типы — через цепочку обработчиков
                    if (!lineChain.handle(type, parts, builder)) {
                        builder.addUnparsed(line);
                    }
                }
            }
        }

        // сброс накопленного состояния (ENEMY_ACTION, CIVILIAN_IMPACT и т.д.)
        lineChain.flushAll(builder);

        return builder.build();
    }

    private String get(String[] parts, int idx) {
        return idx < parts.length ? parts[idx].trim() : null;
    }

    private String extractValue(String token) {
        if (token == null) return null;
        String[] kv = token.split("=");
        return kv.length == 2 ? kv[1].trim() : null;
    }

    private double parseDouble(String v) {
        if (v == null || v.isBlank()) return 0;
        try { return Double.parseDouble(v); } catch (NumberFormatException e) { return 0; }
    }
}
