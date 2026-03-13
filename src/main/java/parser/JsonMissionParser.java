package parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import model.Mission;

import java.io.File;
import java.io.IOException;

public class JsonMissionParser extends MissionParser {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Mission parse(File file) throws IOException {
        validateFile(file);
        return objectMapper.readValue(file, Mission.class);
    }
}

//Для JSON использовал библиотеку Jackson.
//Она сама преобразует JSON-структуру в объект Mission, включая вложенные объекты и коллекции.