package parser.block;

import model.OperationEvent;
import model.builder.MissionBuilder;
import parser.block.util.BlockDataAccessor;

import java.util.List;
import java.util.Map;

public class TimelineBlockParser implements MissionBlockParser {

    @Override
    public boolean accepts(String blockName) {
        return "operationTimeline".equals(blockName);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void parse(Object blockData, MissionBuilder builder) {
        if (!(blockData instanceof List<?> list)) return;
        for (Object item : list) {
            if (item instanceof Map<?,?> map) {
                BlockDataAccessor d = new BlockDataAccessor(map);
                builder.addTimelineEvent(new OperationEvent(
                        d.getString("timestamp"),
                        d.getString("type"),
                        d.getString("description")
                ));
            }
        }
    }
}