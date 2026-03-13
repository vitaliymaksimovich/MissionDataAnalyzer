package parser;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import model.Mission;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class XmlMissionParser extends MissionParser {

    private final XmlMapper xmlMapper = new XmlMapper();

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

        Mission mission = xmlMapper.readValue(file, Mission.class);
        collectUnknownTopLevelFields(file, mission);

        return mission;
    }

    private void collectUnknownTopLevelFields(File file, Mission mission) {
        try {
            Document document = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .parse(file);

            document.getDocumentElement().normalize();

            Node root = document.getDocumentElement();
            NodeList children = root.getChildNodes();

            for (int i = 0; i < children.getLength(); i++) {
                Node node = children.item(i);

                if (node.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }

                String nodeName = node.getNodeName();

                if (!KNOWN_FIELDS.contains(nodeName)) {
                    mission.addUnparsedData(nodeName + ": " + node.getTextContent().trim());
                }
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Ошибка при анализе XML-структуры: " + e.getMessage(), e);
        }
    }
}