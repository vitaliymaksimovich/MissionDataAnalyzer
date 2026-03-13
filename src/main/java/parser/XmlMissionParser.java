package parser;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import model.Mission;

import java.io.File;
import java.io.IOException;

public class XmlMissionParser extends MissionParser {

    private final XmlMapper xmlMapper = new XmlMapper();

    @Override
    public Mission parse(File file) throws IOException {
        validateFile(file);
        return xmlMapper.readValue(file, Mission.class);
    }
}