package com.example.candycrush.model;

public class Tile {

    private String color;
    private String type;

    public Tile() {
    }

    public Tile(String color) {
        this.color = color;
        this.type = "regular";
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
