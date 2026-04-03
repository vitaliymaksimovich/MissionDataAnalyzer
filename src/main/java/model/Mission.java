package model;

import java.util.ArrayList;
import java.util.List;

public class Mission {
    // Обязательные поля
    private String missionId;
    private String date;
    private String location;
    private String outcome;
    private double damageCost;
    private Curse curse;

    // Опциональные стандартные блоки
    private List<Sorcerer> sorcerers = new ArrayList<>();
    private List<Technique> techniques = new ArrayList<>();
    private String comment;

    // Расширенные блоки (Приложение 1)
    private EconomicAssessment economicAssessment;
    private CivilianImpact civilianImpact;
    private EnemyActivity enemyActivity;
    private EnvironmentConditions environmentConditions;
    private List<OperationEvent> operationTimeline = new ArrayList<>();
    private List<String> operationTags = new ArrayList<>();
    private List<String> supportUnits = new ArrayList<>();
    private List<String> recommendations = new ArrayList<>();
    private List<String> notes = new ArrayList<>();
    private List<String> artifactsRecovered = new ArrayList<>();
    private List<String> evacuationZones = new ArrayList<>();
    private List<String> statusEffects = new ArrayList<>();

    // Данные, которые не удалось распознать
    private List<String> unparsedData = new ArrayList<>();

    // --- getters/setters ---

    public String getMissionId() { return missionId; }
    public void setMissionId(String missionId) { this.missionId = missionId; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getOutcome() { return outcome; }
    public void setOutcome(String outcome) { this.outcome = outcome; }
    public double getDamageCost() { return damageCost; }
    public void setDamageCost(double damageCost) { this.damageCost = damageCost; }
    public Curse getCurse() { return curse; }
    public void setCurse(Curse curse) { this.curse = curse; }
    public List<Sorcerer> getSorcerers() { return sorcerers; }
    public void setSorcerers(List<Sorcerer> sorcerers) { this.sorcerers = sorcerers != null ? sorcerers : new ArrayList<>(); }
    public List<Technique> getTechniques() { return techniques; }
    public void setTechniques(List<Technique> techniques) { this.techniques = techniques != null ? techniques : new ArrayList<>(); }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public EconomicAssessment getEconomicAssessment() { return economicAssessment; }
    public void setEconomicAssessment(EconomicAssessment economicAssessment) { this.economicAssessment = economicAssessment; }
    public CivilianImpact getCivilianImpact() { return civilianImpact; }
    public void setCivilianImpact(CivilianImpact civilianImpact) { this.civilianImpact = civilianImpact; }
    public EnemyActivity getEnemyActivity() { return enemyActivity; }
    public void setEnemyActivity(EnemyActivity enemyActivity) { this.enemyActivity = enemyActivity; }
    public EnvironmentConditions getEnvironmentConditions() { return environmentConditions; }
    public void setEnvironmentConditions(EnvironmentConditions environmentConditions) { this.environmentConditions = environmentConditions; }
    public List<OperationEvent> getOperationTimeline() { return operationTimeline; }
    public void setOperationTimeline(List<OperationEvent> operationTimeline) { this.operationTimeline = operationTimeline != null ? operationTimeline : new ArrayList<>(); }
    public List<String> getOperationTags() { return operationTags; }
    public void setOperationTags(List<String> operationTags) { this.operationTags = operationTags; }
    public List<String> getSupportUnits() { return supportUnits; }
    public void setSupportUnits(List<String> supportUnits) { this.supportUnits = supportUnits; }
    public List<String> getRecommendations() { return recommendations; }
    public void setRecommendations(List<String> recommendations) { this.recommendations = recommendations; }
    public List<String> getNotes() { return notes; }
    public void setNotes(List<String> notes) { this.notes = notes; }
    public List<String> getArtifactsRecovered() { return artifactsRecovered; }
    public void setArtifactsRecovered(List<String> artifactsRecovered) { this.artifactsRecovered = artifactsRecovered; }
    public List<String> getEvacuationZones() { return evacuationZones; }
    public void setEvacuationZones(List<String> evacuationZones) { this.evacuationZones = evacuationZones; }
    public List<String> getStatusEffects() { return statusEffects; }
    public void setStatusEffects(List<String> statusEffects) { this.statusEffects = statusEffects; }
    public List<String> getUnparsedData() { return unparsedData; }
    public void setUnparsedData(List<String> unparsedData) { this.unparsedData = unparsedData != null ? unparsedData : new ArrayList<>(); }
    public void addUnparsedData(String value) { if (value != null && !value.isBlank()) unparsedData.add(value); }
}