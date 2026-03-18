package com.example.restaurantpro.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "dining_tables")
public class DiningTable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String tableType;

    private String chairType;

    private Integer capacity;

    @Column(length = 800)
    private String description;

    private String location;

    private boolean active = true;

    public DiningTable() {
    }

    public DiningTable(String name, String tableType, String chairType, Integer capacity, String description, String location, boolean active) {
        this.name = name;
        this.tableType = tableType;
        this.chairType = chairType;
        this.capacity = capacity;
        this.description = description;
        this.location = location;
        this.active = active;
    }

    public String getSummaryLine() {
        return tableType + " - " + chairType + " - Sức chứa " + capacity + " người";
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getTableType() {
        return tableType;
    }

    public String getChairType() {
        return chairType;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public String getDescription() {
        return description;
    }

    public String getLocation() {
        return location;
    }

    public boolean isActive() {
        return active;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setTableType(String tableType) {
        this.tableType = tableType;
    }

    public void setChairType(String chairType) {
        this.chairType = chairType;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
