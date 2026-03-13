package model;

public class Technique {
    private String name;
    private String type;
    private String owner;
    private double damage;

    public Technique() {
    }

    public Technique(String name, String type, String owner, double damage) {
        this.name = name;
        this.type = type;
        this.owner = owner;
        this.damage = damage;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public double getDamage() {
        return damage;
    }

    public void setDamage(double damage) {
        this.damage = damage;
    }

    @Override
    public String toString() {
        return "Technique{" +
                "name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", owner='" + owner + '\'' +
                ", damage=" + damage +
                '}';
    }
}