package parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import model.Mission;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class JsonMissionParser extends MissionParser {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Set<String> KNOWN_FIELDS = new HashSet<>();

    static {
        KNOWN_FIELDS.add("missionId");
        KNOWN_FIELDS.add("date");
        KNOWN_FIELDS.add("location");
        KNOWN_FIELDS.add("outcome");
        KNOWN_FIELDS.add("damageCost");
        KNOWN_FIELDS.add("curse");
        KNOWN_FIELDS.add("sorcerers");
        KNOWN_FIELDS.add("techniques");
        KNOWN_FIELDS.add("comment");
    }

    @Override
    public Mission parse(File file) throws IOException {

        validateFile(file);

        JsonNode root = objectMapper.readTree(file);

        Mission mission = objectMapper.treeToValue(root, Mission.class);

        Iterator<String> fieldNames = root.fieldNames();

        while (fieldNames.hasNext()) {

            String field = fieldNames.next();

            if (!KNOWN_FIELDS.contains(field)) {

                JsonNode valueNode = root.get(field);

                mission.addUnparsedData(field + ": " + valueNode.toString());
            }
        }

        return mission;
    }
}

//Для JSON использовал библиотеку Jackson.
//Она сама преобразует JSON-структуру в объект Mission, включая вложенные объекты и коллекции.