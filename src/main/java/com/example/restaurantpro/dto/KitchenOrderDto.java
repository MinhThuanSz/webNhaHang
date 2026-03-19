package com.example.restaurantpro.dto;

public class KitchenOrderDto {

    private final String tableName;
    private final String menuItemName;
    private final Integer quantity;
    private final String customerNotes;

    public KitchenOrderDto(String tableName, String menuItemName, Integer quantity, String customerNotes) {
        this.tableName = tableName;
        this.menuItemName = menuItemName;
        this.quantity = quantity;
        this.customerNotes = customerNotes;
    }

    public String getTableName() {
        return tableName;
    }

    public String getMenuItemName() {
        return menuItemName;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public String getCustomerNotes() {
        return customerNotes;
    }
}
