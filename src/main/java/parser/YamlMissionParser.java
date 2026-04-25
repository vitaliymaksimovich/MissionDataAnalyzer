package parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class YamlMissionParser extends AbstractStructuredParser {

    private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory());

    @Override
    @SuppressWarnings("unchecked")
    protected Object readRootData(File file) throws IOException {
        return MAPPER.readValue(file, Map.class);
    }
}
