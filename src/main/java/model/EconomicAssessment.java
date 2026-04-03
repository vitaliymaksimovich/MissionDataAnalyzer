package model;

public class EconomicAssessment {
    private double totalDamageCost;
    private double infrastructureDamage;
    private double commercialDamage;
    private double transportDamage;
    private int recoveryEstimateDays;
    private boolean insuranceCovered;

    public double getTotalDamageCost() { return totalDamageCost; }
    public void setTotalDamageCost(double totalDamageCost) { this.totalDamageCost = totalDamageCost; }
    public double getInfrastructureDamage() { return infrastructureDamage; }
    public void setInfrastructureDamage(double infrastructureDamage) { this.infrastructureDamage = infrastructureDamage; }
    public double getCommercialDamage() { return commercialDamage; }
    public void setCommercialDamage(double commercialDamage) { this.commercialDamage = commercialDamage; }
    public double getTransportDamage() { return transportDamage; }
    public void setTransportDamage(double transportDamage) { this.transportDamage = transportDamage; }
    public int getRecoveryEstimateDays() { return recoveryEstimateDays; }
    public void setRecoveryEstimateDays(int recoveryEstimateDays) { this.recoveryEstimateDays = recoveryEstimateDays; }
    public boolean isInsuranceCovered() { return insuranceCovered; }
    public void setInsuranceCovered(boolean insuranceCovered) { this.insuranceCovered = insuranceCovered; }
}