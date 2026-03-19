package com.example.restaurantpro.dto;

public class TableAdminResponseDto {

    private final Long id;
    private final String name;
    private final String tableType;
    private final String floor;
    private final String roomType;
    private final String areaPosition;
    private final String locationDisplay;
    private final Integer capacity;
    private final Integer quantity;
    private final Integer availableQuantity;
    private final boolean active;

    public TableAdminResponseDto(Long id,
                                 String name,
                                 String tableType,
                                 String floor,
                                 String roomType,
                                 String areaPosition,
                                 String locationDisplay,
                                 Integer capacity,
                                 Integer quantity,
                                 Integer availableQuantity,
                                 boolean active) {
        this.id = id;
        this.name = name;
        this.tableType = tableType;
        this.floor = floor;
        this.roomType = roomType;
        this.areaPosition = areaPosition;
        this.locationDisplay = locationDisplay;
        this.capacity = capacity;
        this.quantity = quantity;
        this.availableQuantity = availableQuantity;
        this.active = active;
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

    public String getFloor() {
        return floor;
    }

    public String getRoomType() {
        return roomType;
    }

    public String getAreaPosition() {
        return areaPosition;
    }

    public String getLocationDisplay() {
        return locationDisplay;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public Integer getAvailableQuantity() {
        return availableQuantity;
    }

    public boolean isActive() {
        return active;
    }
}
