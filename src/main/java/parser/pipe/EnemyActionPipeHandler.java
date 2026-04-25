package parser.pipe;

import model.EnemyActivity;
import model.builder.MissionBuilder;

public class EnemyActionPipeHandler implements PipeLineHandler {

    private final EnemyActivity enemyActivity = new EnemyActivity();
    private boolean hasData = false;

    @Override
    public boolean accepts(String lineType) {
        return "ENEMY_ACTION".equals(lineType);
    }

    @Override
    public void handle(String[] parts, MissionBuilder builder) {
        hasData = true;
        // behaviorType берём только первый раз
        if (enemyActivity.getBehaviorType() == null && parts.length > 1) {
            enemyActivity.setBehaviorType(parts[1].trim());
        }
        if (parts.length > 2) {
            enemyActivity.getAttackPatterns().add(parts[2].trim());
        }
    }

    @Override
    public void flush(MissionBuilder builder) {
        if (hasData) builder.enemyActivity(enemyActivity);
    }
}
