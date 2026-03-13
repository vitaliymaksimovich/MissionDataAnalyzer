package parser;

import model.Curse;
import model.Mission;
import model.Sorcerer;
import model.Technique;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;

public class TxtMissionParser extends MissionParser {

    @Override
    public Mission parse(File file) throws IOException {
        String content = readFileContent(file);
        String[] lines = content.split("\\R");

        Mission mission = new Mission();
        Curse curse = new Curse();

        Map<Integer, Sorcerer> sorcererMap = new HashMap<>();
        Map<Integer, Technique> techniqueMap = new HashMap<>();

        Map<String, Consumer<String>> missionHandlers = createMissionHandlers(mission);

        for (String rawLine : lines) {
            String line = rawLine.trim();

            if (line.isEmpty() || !line.contains(":")) {
                continue;
            }

            String[] parts = line.split(":", 2);
            String key = parts[0].trim();
            String value = parts[1].trim();

            if (missionHandlers.containsKey(key)) {
                missionHandlers.get(key).accept(value);
                continue;
            }

            if (key.startsWith("curse.")) {
                if (!parseCurseField(curse, key, value)) {
                    mission.addUnparsedData(line);
                }
                continue;
            }

            if (key.startsWith("sorcerer[")) {
                if (!parseSorcererField(sorcererMap, key, value)) {
                    mission.addUnparsedData(line);
                }
                continue;
            }

            if (key.startsWith("technique[")) {
                if (!parseTechniqueField(techniqueMap, key, value)) {
                    mission.addUnparsedData(line);
                }
                continue;
            }

            mission.addUnparsedData(line);
        }

        mission.setCurse(curse);
        mission.setSorcerers(toSortedList(sorcererMap));
        mission.setTechniques(toSortedList(techniqueMap));

        return mission;
    }

    private Map<String, Consumer<String>> createMissionHandlers(Mission mission) {
        Map<String, Consumer<String>> map = new HashMap<>();

        map.put("missionId", mission::setMissionId);
        map.put("date", mission::setDate);
        map.put("location", mission::setLocation);
        map.put("outcome", mission::setOutcome);
        map.put("note", mission::setComment);
        map.put("damageCost", value -> mission.setDamageCost(Double.parseDouble(value)));

        return map;
    }

    private boolean parseCurseField(Curse curse, String key, String value) {
        switch (key) {
            case "curse.name" -> {
                curse.setName(value);
                return true;
            }
            case "curse.threatLevel" -> {
                curse.setThreatLevel(value);
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    private boolean parseSorcererField(Map<Integer, Sorcerer> sorcererMap, String key, String value) {
        int index = extractIndex(key);
        String fieldName = extractFieldName(key);

        Sorcerer sorcerer = sorcererMap.computeIfAbsent(index, i -> new Sorcerer());

        switch (fieldName) {
            case "name" -> {
                sorcerer.setName(value);
                return true;
            }
            case "rank" -> {
                sorcerer.setRank(value);
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    private boolean parseTechniqueField(Map<Integer, Technique> techniqueMap, String key, String value) {
        int index = extractIndex(key);
        String fieldName = extractFieldName(key);

        Technique technique = techniqueMap.computeIfAbsent(index, i -> new Technique());

        switch (fieldName) {
            case "name" -> {
                technique.setName(value);
                return true;
            }
            case "type" -> {
                technique.setType(value);
                return true;
            }
            case "owner" -> {
                technique.setOwner(value);
                return true;
            }
            case "damage" -> {
                technique.setDamage(Double.parseDouble(value));
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    private int extractIndex(String key) {
        int start = key.indexOf('[');
        int end = key.indexOf(']');

        if (start == -1 || end == -1 || end <= start + 1) {
            throw new IllegalArgumentException("Invalid indexed key: " + key);
        }

        return Integer.parseInt(key.substring(start + 1, end));
    }

    private String extractFieldName(String key) {
        int dotIndex = key.lastIndexOf('.');

        if (dotIndex == -1 || dotIndex == key.length() - 1) {
            throw new IllegalArgumentException("Invalid field key: " + key);
        }

        return key.substring(dotIndex + 1);
    }

    private <T> List<T> toSortedList(Map<Integer, T> map) {
        List<Integer> indexes = new ArrayList<>(map.keySet());
        Collections.sort(indexes);

        List<T> result = new ArrayList<>();
        for (Integer index : indexes) {
            result.add(map.get(index));
        }

        return result;
    }
}