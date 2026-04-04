package parser.block;

import model.EnemyActivity;
import model.builder.MissionBuilder;
import parser.block.util.BlockDataAccessor;

import java.util.List;

public class EnemyActivityBlockParser implements MissionBlockParser {

    @Override
    public boolean accepts(String blockName) {
        return "enemyActivity".equals(blockName);
    }

    @Override
    public void parse(Object blockData, MissionBuilder builder) {
        BlockDataAccessor d = new BlockDataAccessor(blockData);
        EnemyActivity ea = new EnemyActivity();
        ea.setBehaviorType(d.getString("behaviorType"));
        ea.setMobility(d.getString("mobility"));
        ea.setEscalationRisk(d.getString("escalationRisk"));
        ea.setAttackPatterns(d.getStringList("attackPatterns"));
        ea.setCountermeasuresUsed(d.getStringList("countermeasuresUsed"));

        // targetPriority может быть строкой (XML) или списком (JSON/YAML)
        List<String> targets = d.getStringList("targetPriority");
        if (targets.isEmpty()) {
            String single = d.getString("targetPriority");
            if (single != null) targets = List.of(single);
        }
        ea.setTargetPriority(targets);

        builder.enemyActivity(ea);
    }
}