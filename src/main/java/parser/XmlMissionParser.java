package parser;

import model.Curse;
import model.Mission;
import model.Sorcerer;
import model.Technique;
import model.builder.MissionBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import parser.block.BlockParserChain;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.IOException;
import java.util.Set;

public class XmlMissionParser extends MissionParser {

    private static final Set<String> CORE_FIELDS = Set.of(
            "missionId", "date", "location", "outcome", "damageCost",
            "curse", "sorcerers", "techniques", "comment"
    );

    private final BlockParserChain blockChain;

    public XmlMissionParser() {
        this.blockChain = BlockParserChain.createDefault();
    }

    @Override
    public Mission parse(File file) throws IOException {
        Document doc = buildDocument(file);
        Element root = doc.getDocumentElement();
        MissionBuilder builder = new MissionBuilder();

        builder.missionId(child(root, "missionId"))
                .date(child(root, "date"))
                .location(child(root, "location"))
                .outcome(child(root, "outcome"))
                .damageCost(parseDouble(child(root, "damageCost")))
                .comment(child(root, "comment"));

        NodeList children = root.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE) continue;
            String tag = node.getNodeName();

            switch (tag) {
                case "curse"      -> parseCurse((Element) node, builder);
                case "sorcerers"  -> parseSorcerers((Element) node, builder);
                case "techniques" -> parseTechniques((Element) node, builder);
                default -> {
                    if (!CORE_FIELDS.contains(tag)) {
                        if (!blockChain.handle(tag, node, builder)) {
                            builder.addUnparsed(tag + ": " + node.getTextContent().trim());
                        }
                    }
                }
            }
        }

        return builder.build();
    }

    private void parseCurse(Element el, MissionBuilder builder) {
        // поддержка как <name>, так и <n>
        String name = child(el, "name");
        if (name == null) name = child(el, "n");
        builder.curse(new Curse(name, child(el, "threatLevel")));
    }

    private void parseSorcerers(Element el, MissionBuilder builder) {
        NodeList list = el.getElementsByTagName("sorcerer");
        for (int i = 0; i < list.getLength(); i++) {
            Element s = (Element) list.item(i);
            String name = child(s, "name");
            if (name == null) name = child(s, "n");
            builder.addSorcerer(new Sorcerer(name, child(s, "rank")));
        }
    }

    private void parseTechniques(Element el, MissionBuilder builder) {
        NodeList list = el.getElementsByTagName("technique");
        for (int i = 0; i < list.getLength(); i++) {
            Element t = (Element) list.item(i);
            String name = child(t, "name");
            if (name == null) name = child(t, "n");
            builder.addTechnique(new Technique(
                    name,
                    child(t, "type"),
                    child(t, "owner"),
                    parseDouble(child(t, "damage"))
            ));
        }
    }

    private String child(Element el, String tag) {
        NodeList list = el.getElementsByTagName(tag);
        if (list.getLength() == 0) return null;
        // берём только прямого потомка, не вложенного
        for (int i = 0; i < list.getLength(); i++) {
            if (list.item(i).getParentNode() == el) {
                return list.item(i).getTextContent().trim();
            }
        }
        return null;
    }

    private double parseDouble(String v) {
        if (v == null || v.isBlank()) return 0;
        try { return Double.parseDouble(v); } catch (NumberFormatException e) { return 0; }
    }

    private Document buildDocument(File file) throws IOException {
        try {
            return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file);
        } catch (Exception e) {
            throw new IOException("Ошибка разбора XML: " + e.getMessage(), e);
        }
    }
}