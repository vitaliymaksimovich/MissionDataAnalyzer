package parser;

import model.Curse;
import model.CivilianImpact;
import model.Mission;
import model.OperationEvent;
import model.EnemyActivity;
import model.Sorcerer;
import model.Technique;
import model.builder.MissionBuilder;

import java.io.File;
import java.io.IOException;

public class PipeMissionParser extends MissionParser {

    @Override
    public Mission parse(File file) throws IOException {
        validate(file);
        String[] lines = readFile(file).split("\\R");
        MissionBuilder builder = new MissionBuilder();
        CivilianImpact civilianImpact = new CivilianImpact();
        EnemyActivity enemyActivity = new EnemyActivity();
        boolean hasCivilianImpact = false;
        boolean hasEnemyActivity = false;

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

                case "TIMELINE_EVENT" -> builder.addTimelineEvent(
                        new OperationEvent(get(parts, 1), get(parts, 2), get(parts, 3))
                );

                case "MISSION_RESULT" -> builder
                        .outcome(get(parts, 1))
                        .damageCost(parseDouble(extractValue(get(parts, 2))));

                case "CIVILIAN_IMPACT" -> {
                    hasCivilianImpact = true;
                    for (int i = 1; i < parts.length; i++) {
                        String[] kv = parts[i].split("=");
                        if (kv.length != 2) continue;
                        switch (kv[0].trim()) {
                            case "evacuated" -> civilianImpact.setEvacuated(parseInt(kv[1]));
                            case "injured"   -> civilianImpact.setInjured(parseInt(kv[1]));
                            case "missing"   -> civilianImpact.setMissing(parseInt(kv[1]));
                        }
                    }
                }

                case "ENEMY_ACTION" -> {
                    hasEnemyActivity = true;
                    String pattern = get(parts, 2);
                    // behaviorType берём только первый раз
                    if (enemyActivity.getBehaviorType() == null) {
                        enemyActivity.setBehaviorType(get(parts, 1));
                    }
                    if (pattern != null) enemyActivity.getAttackPatterns().add(pattern);
                }

                default -> builder.addUnparsed(line);
            }
        }

        if (hasCivilianImpact) builder.civilianImpact(civilianImpact);
        if (hasEnemyActivity) builder.enemyActivity(enemyActivity);

        return builder.build();
    }

    private String get(String[] parts, int idx) {
        return idx < parts.length ? parts[idx].trim() : null;
    }

    // из "damageCost=2100000" вытаскивает "2100000"
    private String extractValue(String token) {
        if (token == null) return null;
        String[] kv = token.split("=");
        return kv.length == 2 ? kv[1].trim() : null;
    }

    private double parseDouble(String v) {
        if (v == null || v.isBlank()) return 0;
        try { return Double.parseDouble(v); } catch (NumberFormatException e) { return 0; }
    }

    private int parseInt(String v) {
        if (v == null || v.isBlank()) return 0;
        try { return Integer.parseInt(v.trim()); } catch (NumberFormatException e) { return 0; }
    }
}