package com.weighttracker.app;

import java.io.Serializable;
import java.util.Date;

/**
 * Data model representing a single weight entry.
 */
public class WeightEntry implements Serializable {

    private long id; // database primary key, -1 for unsaved
    private Date date;
    private double weight;
    private boolean isMetric; // true = kg, false = lbs
    private String photoPath; // nullable, file path to progress photo

    public WeightEntry(Date date, double weight, boolean isMetric) {
        this.id = -1;
        this.date = date;
        this.weight = weight;
        this.isMetric = isMetric;
        this.photoPath = null;
    }

    public WeightEntry(long id, Date date, double weight, boolean isMetric, String photoPath) {
        this.id = id;
        this.date = date;
        this.weight = weight;
        this.isMetric = isMetric;
        this.photoPath = photoPath;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public boolean isMetric() {
        return isMetric;
    }

    public void setMetric(boolean metric) {
        isMetric = metric;
    }

    public String getPhotoPath() {
        return photoPath;
    }

    public void setPhotoPath(String photoPath) {
        this.photoPath = photoPath;
    }

    /**
     * Returns weight formatted to one decimal place with the appropriate unit.
     */
    public String getFormattedWeight() {
        String unit = isMetric ? "kg" : "lbs";
        return String.format("%.1f %s", weight, unit);
    }
}
