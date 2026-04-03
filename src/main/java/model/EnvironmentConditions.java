package model;

public class EnvironmentConditions {
    private String weather;
    private String timeOfDay;
    private String visibility;
    private double cursedEnergyDensity;

    public String getWeather() { return weather; }
    public void setWeather(String weather) { this.weather = weather; }
    public String getTimeOfDay() { return timeOfDay; }
    public void setTimeOfDay(String timeOfDay) { this.timeOfDay = timeOfDay; }
    public String getVisibility() { return visibility; }
    public void setVisibility(String visibility) { this.visibility = visibility; }
    public double getCursedEnergyDensity() { return cursedEnergyDensity; }
    public void setCursedEnergyDensity(double cursedEnergyDensity) { this.cursedEnergyDensity = cursedEnergyDensity; }
}