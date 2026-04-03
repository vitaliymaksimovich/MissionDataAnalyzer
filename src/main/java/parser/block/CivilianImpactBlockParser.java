package parser.block;

import model.CivilianImpact;
import model.builder.MissionBuilder;
import parser.block.util.BlockDataAccessor;

public class CivilianImpactBlockParser implements MissionBlockParser {

    @Override
    public boolean accepts(String blockName) {
        return "civilianImpact".equals(blockName);
    }

    @Override
    public void parse(Object blockData, MissionBuilder builder) {
        BlockDataAccessor d = new BlockDataAccessor(blockData);
        CivilianImpact ci = new CivilianImpact();
        ci.setEvacuated(d.getInt("evacuated"));
        ci.setInjured(d.getInt("injured"));
        ci.setMissing(d.getInt("missing"));
        ci.setPublicExposureRisk(d.getString("publicExposureRisk"));
        builder.civilianImpact(ci);
    }
}