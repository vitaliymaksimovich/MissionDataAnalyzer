package model;

public class Sorcerer {
    private String name;
    private String rank;

    public Sorcerer() {
    }

    public Sorcerer(String name, String rank) {
        this.name = name;
        this.rank = rank;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRank() {
        return rank;
    }

    public void setRank(String rank) {
        this.rank = rank;
    }

    @Override
    public String toString() {
        return "Sorcerer{" +
                "name='" + name + '\'' +
                ", rank='" + rank + '\'' +
                '}';
    }
}
