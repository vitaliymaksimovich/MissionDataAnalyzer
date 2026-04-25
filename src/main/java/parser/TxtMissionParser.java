package parser;

import model.Curse;
import model.Mission;
import model.Sorcerer;
import model.Technique;
import model.builder.MissionBuilder;
import parser.block.BlockParserChain;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class TxtMissionParser extends MissionParser {

    /** Секции, обрабатываемые напрямую (не через blockChain) */
    private static final Set<String> CORE_SECTIONS = Set.of(
            "MISSION", "CURSE", "SORCERER", "TECHNIQUE"
    );

    private final BlockParserChain blockChain;

    public TxtMissionParser() {
        this.blockChain = BlockParserChain.createDefault();
    }

    @Override
    public Mission parse(File file) throws IOException {
        String[] lines = readFile(file).split("\\R");
        MissionBuilder builder = new MissionBuilder();

        if (hasSections(lines)) {
            parseSectionFormat(lines, builder);
        } else {
            parseFlatFormat(lines, builder);
        }

        return builder.build();
    }

    // --- формат: [SECTION] / key=value ---

    private void parseSectionFormat(String[] lines, MissionBuilder builder) {
        String section = "";
        Sorcerer currentSorcerer = null;
        Technique currentTechnique = null;
        // данные неизвестных секций для делегирования в blockChain
        Map<String, String> blockData = new HashMap<>();
        String blockSectionName = null;

        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty()) continue;

            if (line.startsWith("[") && line.endsWith("]")) {
                // завершаем предыдущие объекты
                if (currentSorcerer != null) { builder.addSorcerer(currentSorcerer); currentSorcerer = null; }
                if (currentTechnique != null) { builder.addTechnique(currentTechnique); currentTechnique = null; }
                flushBlockData(blockSectionName, blockData, builder);

                section = line.substring(1, line.length() - 1).toUpperCase();
                if ("SORCERER".equals(section)) currentSorcerer = new Sorcerer();
                if ("TECHNIQUE".equals(section)) currentTechnique = new Technique();

                // если секция не core — готовимся к накоплению данных для blockChain
                if (!CORE_SECTIONS.contains(section)) {
                    blockSectionName = toBlockName(section);
                }
                continue;
            }

            if (!line.contains("=")) continue;
            String[] parts = line.split("=", 2);
            String key = parts[0].trim();
            String val = parts[1].trim();

            if (CORE_SECTIONS.contains(section)) {
                switch (section) {
                    case "MISSION" -> applyMissionField(key, val, builder);
                    case "CURSE"   -> applyCurseField(key, val, builder);
                    case "SORCERER" -> {
                        if (currentSorcerer == null) currentSorcerer = new Sorcerer();
                        applySorcererField(key, val, currentSorcerer);
                    }
                    case "TECHNIQUE" -> {
                        if (currentTechnique == null) currentTechnique = new Technique();
                        applyTechniqueField(key, val, currentTechnique);
                    }
                }
            } else {
                // неизвестная секция — накапливаем для blockChain
                blockData.put(key, val);
            }
        }

        // сбрасываем последние объекты
        if (currentSorcerer != null) builder.addSorcerer(currentSorcerer);
        if (currentTechnique != null) builder.addTechnique(currentTechnique);
        flushBlockData(blockSectionName, blockData, builder);
    }

    /** Сбрасывает накопленные данные секции в blockChain */
    private void flushBlockData(String blockName, Map<String, String> data, MissionBuilder builder) {
        if (blockName != null && !data.isEmpty()) {
            if (!blockChain.handle(blockName, data, builder)) {
                data.forEach((k, v) -> builder.addUnparsed(k + ": " + v));
            }
        }
        data.clear();
    }

    /** Конвертирует UPPER_SNAKE_CASE → camelCase: ENVIRONMENT → environment, CIVILIAN_IMPACT → civilianImpact */
    private String toBlockName(String section) {
        String[] parts = section.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            sb.append(Character.toUpperCase(parts[i].charAt(0)));
            sb.append(parts[i].substring(1));
        }
        return sb.toString();
    }

    // --- формат: key: value / key[i].field: value ---

    private void parseFlatFormat(String[] lines, MissionBuilder builder) {
        Map<Integer, Sorcerer> sorcerers = new HashMap<>();
        Map<Integer, Technique> techniques = new HashMap<>();

        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty() || !line.contains(":")) continue;

            String[] parts = line.split(":", 2);
            String key = parts[0].trim();
            String val = parts[1].trim();

            if (key.startsWith("sorcerer[")) {
                int idx = extractIndex(key);
                String field = extractField(key);
                Sorcerer s = sorcerers.computeIfAbsent(idx, i -> new Sorcerer());
                applySorcererField(field, val, s);
            } else if (key.startsWith("technique[")) {
                int idx = extractIndex(key);
                String field = extractField(key);
                Technique t = techniques.computeIfAbsent(idx, i -> new Technique());
                applyTechniqueField(field, val, t);
            } else if (key.startsWith("curse.")) {
                applyCurseField(key.substring(6), val, builder);
            } else {
                applyMissionField(key, val, builder);
            }
        }

        sorcerers.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> builder.addSorcerer(e.getValue()));
        techniques.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> builder.addTechnique(e.getValue()));
    }

    // --- вспомогательные методы ---

    private void applyMissionField(String key, String val, MissionBuilder builder) {
        switch (key) {
            case "missionId"  -> builder.missionId(val);
            case "date"       -> builder.date(val);
            case "location"   -> builder.location(val);
            case "outcome"    -> builder.outcome(val);
            case "damageCost" -> builder.damageCost(parseDouble(val));
            case "note", "comment" -> builder.comment(val);
            default           -> builder.addUnparsed(key + ": " + val);
        }
    }

    private void applyCurseField(String key, String val, MissionBuilder builder) {
        curseBuffer.put(key, val);
        flushCurse(builder);
    }

    private final Map<String, String> curseBuffer = new HashMap<>();

    private void flushCurse(MissionBuilder builder) {
        if (curseBuffer.containsKey("name") || curseBuffer.containsKey("threatLevel")) {
            builder.curse(new Curse(
                    curseBuffer.get("name"),
                    curseBuffer.get("threatLevel")
            ));
        }
    }

    private void applySorcererField(String key, String val, Sorcerer s) {
        switch (key) {
            case "name" -> s.setName(val);
            case "rank" -> s.setRank(val);
        }
    }

    private void applyTechniqueField(String key, String val, Technique t) {
        switch (key) {
            case "name"   -> t.setName(val);
            case "type"   -> t.setType(val);
            case "owner"  -> t.setOwner(val);
            case "damage" -> t.setDamage(parseDouble(val));
        }
    }

    private boolean hasSections(String[] lines) {
        for (String line : lines) {
            String t = line.trim();
            if (t.startsWith("[") && t.endsWith("]")) return true;
        }
        return false;
    }

    private int extractIndex(String key) {
        return Integer.parseInt(key.substring(key.indexOf('[') + 1, key.indexOf(']')));
    }

    private String extractField(String key) {
        return key.substring(key.lastIndexOf('.') + 1);
    }

    private double parseDouble(String v) {
        try { return Double.parseDouble(v); } catch (NumberFormatException e) { return 0; }
    }
}
