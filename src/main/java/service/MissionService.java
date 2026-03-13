package service;

import model.Mission;
import parser.MissionParser;
import parser.ParserFactory;

import java.io.File;
import java.io.IOException;

public class MissionService {

    public Mission loadMission(File file) throws IOException {
        if (file == null) {
            throw new IllegalArgumentException("File must not be null.");
        }

        MissionParser parser = ParserFactory.createParser(file);
        return parser.parse(file);
    }
}