package com.school.model;

public class FeeItem {

    private int    id;
    private int    feeStructureId;
    private String itemName;
    private double amount;

    public FeeItem() {}

    public FeeItem(int feeStructureId, String itemName, double amount) {
        this.feeStructureId = feeStructureId;
        this.itemName       = itemName;
        this.amount         = amount;
    }

    // Getters / Setters
    public int getId()                          { return id; }
    public void setId(int id)                   { this.id = id; }

    public int getFeeStructureId()              { return feeStructureId; }
    public void setFeeStructureId(int v)        { this.feeStructureId = v; }

    public String getItemName()                 { return itemName; }
    public void setItemName(String v)           { this.itemName = v; }

    public double getAmount()                   { return amount; }
    public void setAmount(double v)             { this.amount = v; }
}
