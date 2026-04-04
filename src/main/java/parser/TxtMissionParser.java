package parser;

import model.Curse;
import model.Mission;
import model.OperationEvent;
import model.Sorcerer;
import model.Technique;
import model.builder.MissionBuilder;
import parser.block.BlockParserChain;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class TxtMissionParser extends MissionParser {

    private final BlockParserChain blockChain;

    public TxtMissionParser() {
        this.blockChain = BlockParserChain.createDefault();
    }

    @Override
    public Mission parse(File file) throws IOException {
        String[] lines = readFile(file).split("\\R");
        MissionBuilder builder = new MissionBuilder();

        // определяем формат по наличию [SECTION]-заголовков
        boolean isSectionFormat = hasSections(lines);

        if (isSectionFormat) {
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
        Map<String, String> envMap = new HashMap<>();

        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty()) continue;

            if (line.startsWith("[") && line.endsWith("]")) {
                // завершаем предыдущие объекты
                if (currentSorcerer != null) { builder.addSorcerer(currentSorcerer); currentSorcerer = null; }
                if (currentTechnique != null) { builder.addTechnique(currentTechnique); currentTechnique = null; }
                if (!envMap.isEmpty()) {
                    blockChain.handle("environment", envMap, builder);
                    envMap = new HashMap<>();
                }
                section = line.substring(1, line.length() - 1).toUpperCase();
                if ("SORCERER".equals(section)) currentSorcerer = new Sorcerer();
                if ("TECHNIQUE".equals(section)) currentTechnique = new Technique();
                continue;
            }

            if (!line.contains("=")) continue;
            String[] parts = line.split("=", 2);
            String key = parts[0].trim();
            String val = parts[1].trim();

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
                case "ENVIRONMENT" -> envMap.put(key, val);
                default -> builder.addUnparsed(line);
            }
        }

        // не забываем последние объекты
        if (currentSorcerer != null) builder.addSorcerer(currentSorcerer);
        if (currentTechnique != null) builder.addTechnique(currentTechnique);
        if (!envMap.isEmpty()) blockChain.handle("environment", envMap, builder);
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
        // достаём или создаём curse через builder
        // храним временно в локальном состоянии через addUnparsed-trick не нужен —
        // используем отдельный вспомогательный метод с состоянием через поля класса
        // но чище — разобрать curse полностью перед билдом:
        // здесь просто накапливаем в builder через специальный промежуточный объект
        curseBuffer.put(key, val);
        flushCurse(builder);
    }

    // буфер для сборки Curse из плоских строк
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