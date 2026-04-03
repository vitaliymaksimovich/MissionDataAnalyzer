package model.builder;

import model.*;

import java.util.ArrayList;
import java.util.List;

public class MissionBuilder {

    private final Mission mission = new Mission();

    // --- Основные поля ---
    public MissionBuilder missionId(String v)   { mission.setMissionId(v); return this; }
    public MissionBuilder date(String v)         { mission.setDate(v); return this; }
    public MissionBuilder location(String v)     { mission.setLocation(v); return this; }
    public MissionBuilder outcome(String v)      { mission.setOutcome(v); return this; }
    public MissionBuilder damageCost(double v)   { mission.setDamageCost(v); return this; }
    public MissionBuilder comment(String v)      { mission.setComment(v); return this; }
    public MissionBuilder curse(Curse v)         { mission.setCurse(v); return this; }

    // --- Коллекции участников и техник ---
    public MissionBuilder addSorcerer(Sorcerer v)   { mission.getSorcerers().add(v); return this; }
    public MissionBuilder addTechnique(Technique v) { mission.getTechniques().add(v); return this; }

    // --- Расширенные блоки ---
    public MissionBuilder economicAssessment(EconomicAssessment v) { mission.setEconomicAssessment(v); return this; }
    public MissionBuilder civilianImpact(CivilianImpact v)         { mission.setCivilianImpact(v); return this; }
    public MissionBuilder enemyActivity(EnemyActivity v)           { mission.setEnemyActivity(v); return this; }
    public MissionBuilder environmentConditions(EnvironmentConditions v) { mission.setEnvironmentConditions(v); return this; }

    // --- Списочные блоки ---
    public MissionBuilder addTimelineEvent(OperationEvent v) { mission.getOperationTimeline().add(v); return this; }
    public MissionBuilder addTag(String v)                   { mission.getOperationTags().add(v); return this; }
    public MissionBuilder addSupportUnit(String v)           { mission.getSupportUnits().add(v); return this; }
    public MissionBuilder addRecommendation(String v)        { mission.getRecommendations().add(v); return this; }
    public MissionBuilder addNote(String v)                  { mission.getNotes().add(v); return this; }
    public MissionBuilder addArtifact(String v)              { mission.getArtifactsRecovered().add(v); return this; }
    public MissionBuilder addEvacuationZone(String v)        { mission.getEvacuationZones().add(v); return this; }
    public MissionBuilder addStatusEffect(String v)          { mission.getStatusEffects().add(v); return this; }
    public MissionBuilder addUnparsed(String v)              { mission.addUnparsedData(v); return this; }

    public Mission build() { return mission; }
}