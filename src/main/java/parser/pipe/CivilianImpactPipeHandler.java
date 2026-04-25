package parser.pipe;

import model.CivilianImpact;
import model.builder.MissionBuilder;
import parser.util.NumberParser;

public class CivilianImpactPipeHandler implements PipeLineHandler {

    private final CivilianImpact civilianImpact = new CivilianImpact();
    private boolean hasData = false;

    @Override
    public boolean accepts(String lineType) {
        return "CIVILIAN_IMPACT".equals(lineType);
    }

    @Override
    public void handle(String[] parts, MissionBuilder builder) {
        hasData = true;
        for (int i = 1; i < parts.length; i++) {
            String[] kv = parts[i].split("=");
            if (kv.length != 2) continue;
            switch (kv[0].trim()) {
                case "evacuated" -> civilianImpact.setEvacuated(NumberParser.parseIntOrZero(kv[1]));
                case "injured"   -> civilianImpact.setInjured(NumberParser.parseIntOrZero(kv[1]));
                case "missing"   -> civilianImpact.setMissing(NumberParser.parseIntOrZero(kv[1]));
            }
        }
    }

    @Override
    public void flush(MissionBuilder builder) {
        if (hasData) builder.civilianImpact(civilianImpact);
    }
}
