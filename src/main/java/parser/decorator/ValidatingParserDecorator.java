package parser.decorator;

import model.Mission;
import parser.MissionParser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ValidatingParserDecorator extends MissionParser {

    private final MissionParser delegate;

    public ValidatingParserDecorator(MissionParser delegate) {
        this.delegate = delegate;
    }

    @Override
    public Mission parse(File file) throws IOException {
        Mission mission = delegate.parse(file);
        validate(mission);
        return mission;
    }

    private void validate(Mission mission) {
        List<String> missing = new ArrayList<>();

        if (isBlank(mission.getMissionId())) missing.add("missionId");
        if (isBlank(mission.getDate()))      missing.add("date");
        if (isBlank(mission.getLocation()))  missing.add("location");
        if (isBlank(mission.getOutcome()))   missing.add("outcome");
        if (mission.getCurse() == null)      missing.add("curse");
        else {
            if (isBlank(mission.getCurse().getName()))       missing.add("curse.name");
            if (isBlank(mission.getCurse().getThreatLevel())) missing.add("curse.threatLevel");
        }

        missing.forEach(field ->
                mission.addUnparsedData("[ВНИМАНИЕ] Обязательное поле отсутствует: " + field)
        );
    }

    private boolean isBlank(String v) {
        return v == null || v.isBlank();
    }
}