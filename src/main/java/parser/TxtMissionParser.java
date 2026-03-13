package parser;

import model.Curse;
import model.Mission;
import model.Sorcerer;
import model.Technique;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Consumer;

public class TxtMissionParser extends MissionParser {

    @Override
    public Mission parse(File file) throws IOException {

        String content = readFileContent(file);
        String[] lines = content.split("\\R");

        Mission mission = new Mission();
        Curse curse = new Curse();
        List<Sorcerer> sorcerers = new ArrayList<>();
        List<Technique> techniques = new ArrayList<>();

        Map<String, Consumer<String>> missionHandlers = createMissionHandlers(mission);
        Map<String, Consumer<String>> curseHandlers = createCurseHandlers(curse);

        String section = "mission";

        for (String rawLine : lines) {

            String line = rawLine.trim();

            if (line.isEmpty()) continue;

            if (line.equalsIgnoreCase("curse:")) {
                section = "curse";
                continue;
            }

            if (line.equalsIgnoreCase("sorcerers:")) {
                section = "sorcerers";
                continue;
            }

            if (line.equalsIgnoreCase("techniques:")) {
                section = "techniques";
                continue;
            }

            if (section.equals("sorcerers") && line.startsWith("-")) {
                sorcerers.add(parseSorcerer(line));
                continue;
            }

            if (section.equals("techniques") && line.startsWith("-")) {
                techniques.add(parseTechnique(line));
                continue;
            }

            if (!line.contains(":")) continue;

            String[] parts = line.split(":", 2);
            String key = parts[0].trim();
            String value = parts[1].trim();

            if (section.equals("mission")) {
                Consumer<String> handler = missionHandlers.get(key);
                if (handler != null) handler.accept(value);
            }

            if (section.equals("curse")) {
                Consumer<String> handler = curseHandlers.get(key);
                if (handler != null) handler.accept(value);
            }
        }

        mission.setCurse(curse);
        mission.setSorcerers(sorcerers);
        mission.setTechniques(techniques);

        return mission;
    }

    private Map<String, Consumer<String>> createMissionHandlers(Mission mission) {

        Map<String, Consumer<String>> map = new HashMap<>();

        map.put("missionId", mission::setMissionId);
        map.put("location", mission::setLocation);
        map.put("outcome", mission::setOutcome);
        map.put("date", v -> mission.setDate(LocalDate.parse(v)));
        map.put("damageCost", v -> mission.setDamageCost(Double.parseDouble(v)));

        return map;
    }

    private Map<String, Consumer<String>> createCurseHandlers(Curse curse) {

        Map<String, Consumer<String>> map = new HashMap<>();

        map.put("name", curse::setName);
        map.put("threatLevel", curse::setThreatLevel);

        return map;
    }

    private Sorcerer parseSorcerer(String line) {

        String data = line.substring(1).trim();
        String[] parts = data.split(",");

        String name = "";
        String rank = "";

        for (String part : parts) {

            String[] pair = part.split(":", 2);
            String key = pair[0].trim();
            String value = pair[1].trim();

            if (key.equals("name")) name = value;
            if (key.equals("rank")) rank = value;
        }

        return new Sorcerer(name, rank);
    }

    private Technique parseTechnique(String line) {

        String data = line.substring(1).trim();
        String[] parts = data.split(",");

        String name = "";
        String type = "";
        String owner = "";
        double damage = 0;

        for (String part : parts) {

            String[] pair = part.split(":", 2);
            String key = pair[0].trim();
            String value = pair[1].trim();

            if (key.equals("name")) name = value;
            if (key.equals("type")) type = value;
            if (key.equals("owner")) owner = value;
            if (key.equals("damage")) damage = Double.parseDouble(value);
        }

        return new Technique(name, type, owner, damage);
    }
}