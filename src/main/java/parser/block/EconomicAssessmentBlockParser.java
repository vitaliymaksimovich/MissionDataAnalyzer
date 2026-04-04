package parser.block;

import model.EconomicAssessment;
import model.builder.MissionBuilder;
import parser.block.util.BlockDataAccessor;

public class EconomicAssessmentBlockParser implements MissionBlockParser {

    @Override
    public boolean accepts(String blockName) {
        return "economicAssessment".equals(blockName);
    }

    @Override
    public void parse(Object blockData, MissionBuilder builder) {
        BlockDataAccessor d = new BlockDataAccessor(blockData);
        EconomicAssessment ea = new EconomicAssessment();
        ea.setTotalDamageCost(d.getDouble("totalDamageCost"));
        ea.setInfrastructureDamage(d.getDouble("infrastructureDamage"));
        ea.setCommercialDamage(d.getDouble("commercialDamage"));
        ea.setTransportDamage(d.getDouble("transportDamage"));
        ea.setRecoveryEstimateDays(d.getInt("recoveryEstimateDays"));
        ea.setInsuranceCovered(d.getBoolean("insuranceCovered"));
        builder.economicAssessment(ea);
    }
}