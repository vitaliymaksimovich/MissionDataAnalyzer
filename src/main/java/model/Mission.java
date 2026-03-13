package model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.ArrayList;
import java.util.List;

public class Mission {
    private String missionId;
    private String date;
    private String location;
    private String outcome;
    private double damageCost;
    private Curse curse;

    @JacksonXmlElementWrapper(localName = "sorcerers")
    @JacksonXmlProperty(localName = "sorcerer")
    private List<Sorcerer> sorcerers;

    @JacksonXmlElementWrapper(localName = "techniques")
    @JacksonXmlProperty(localName = "technique")
    private List<Technique> techniques;

    public Mission() {
        this.sorcerers = new ArrayList<>();
        this.techniques = new ArrayList<>();
    }

    public Mission(String missionId, String date, String location, String outcome,
                   double damageCost, Curse curse,
                   List<Sorcerer> sorcerers, List<Technique> techniques) {
        this.missionId = missionId;
        this.date = date;
        this.location = location;
        this.outcome = outcome;
        this.damageCost = damageCost;
        this.curse = curse;
        this.sorcerers = sorcerers != null ? sorcerers : new ArrayList<>();
        this.techniques = techniques != null ? techniques : new ArrayList<>();
    }

    public String getMissionId() {
        return missionId;
    }

    public void setMissionId(String missionId) {
        this.missionId = missionId;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getOutcome() {
        return outcome;
    }

    public void setOutcome(String outcome) {
        this.outcome = outcome;
    }

    public double getDamageCost() {
        return damageCost;
    }

    public void setDamageCost(double damageCost) {
        this.damageCost = damageCost;
    }

    public Curse getCurse() {
        return curse;
    }

    public void setCurse(Curse curse) {
        this.curse = curse;
    }

    public List<Sorcerer> getSorcerers() {
        return sorcerers;
    }

    public void setSorcerers(List<Sorcerer> sorcerers) {
        this.sorcerers = sorcerers != null ? sorcerers : new ArrayList<>();
    }

    public List<Technique> getTechniques() {
        return techniques;
    }

    public void setTechniques(List<Technique> techniques) {
        this.techniques = techniques != null ? techniques : new ArrayList<>();
    }

    @Override
    public String toString() {
        return "Mission{" +
                "missionId='" + missionId + '\'' +
                ", date=" + date +
                ", location='" + location + '\'' +
                ", outcome='" + outcome + '\'' +
                ", damageCost=" + damageCost +
                ", curse=" + curse +
                ", sorcerers=" + sorcerers +
                ", techniques=" + techniques +
                '}';
    }
}
