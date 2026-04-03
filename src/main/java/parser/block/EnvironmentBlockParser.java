package parser.block;

import model.EnvironmentConditions;
import model.builder.MissionBuilder;
import parser.block.util.BlockDataAccessor;

public class EnvironmentBlockParser implements MissionBlockParser {

    @Override
    public boolean accepts(String blockName) {
        return "environmentConditions".equals(blockName)
                || "environment".equals(blockName);
    }

    @Override
    public void parse(Object blockData, MissionBuilder builder) {
        BlockDataAccessor d = new BlockDataAccessor(blockData);
        EnvironmentConditions ec = new EnvironmentConditions();
        ec.setWeather(d.getString("weather"));
        ec.setTimeOfDay(d.getString("timeOfDay"));
        ec.setVisibility(d.getString("visibility"));
        ec.setCursedEnergyDensity(d.getDouble("cursedEnergyDensity"));
        builder.environmentConditions(ec);
    }
}