package model;

public class CivilianImpact {
    private int evacuated;
    private int injured;
    private int missing;
    private String publicExposureRisk;

    public int getEvacuated() { return evacuated; }
    public void setEvacuated(int evacuated) { this.evacuated = evacuated; }
    public int getInjured() { return injured; }
    public void setInjured(int injured) { this.injured = injured; }
    public int getMissing() { return missing; }
    public void setMissing(int missing) { this.missing = missing; }
    public String getPublicExposureRisk() { return publicExposureRisk; }
    public void setPublicExposureRisk(String publicExposureRisk) { this.publicExposureRisk = publicExposureRisk; }
}
