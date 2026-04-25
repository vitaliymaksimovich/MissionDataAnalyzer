package parser.pipe;

import model.OperationEvent;
import model.builder.MissionBuilder;

public class TimelinePipeHandler implements PipeLineHandler {

    @Override
    public boolean accepts(String lineType) {
        return "TIMELINE_EVENT".equals(lineType);
    }

    @Override
    public void handle(String[] parts, MissionBuilder builder) {
        builder.addTimelineEvent(new OperationEvent(
                get(parts, 1), get(parts, 2), get(parts, 3)
        ));
    }

    private String get(String[] parts, int idx) {
        return idx < parts.length ? parts[idx].trim() : null;
    }
}
