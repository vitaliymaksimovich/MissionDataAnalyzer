package parser;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.IOException;

public class XmlMissionParser extends AbstractStructuredParser {

    @Override
    protected Object readRootData(File file) throws IOException {
        try {
            return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
                    .getDocumentElement();
        } catch (Exception e) {
            throw new IOException("Ошибка разбора XML: " + e.getMessage(), e);
        }
    }
}
