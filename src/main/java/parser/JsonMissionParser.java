package parser;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;

public class JsonMissionParser extends AbstractStructuredParser {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    protected Object readRootData(File file) throws IOException {
        return MAPPER.readTree(file);
    }
}
