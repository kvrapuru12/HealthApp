package com.healthapp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnore;

public class HeightInput {
    
    @JsonProperty("value")
    private Double value;
    
    @JsonProperty("unit")
    private HeightUnit unit;
    
    public enum HeightUnit {
        CM("cm"),
        FEET("feet");
        
        private final String displayName;
        
        HeightUnit(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    // Default constructor
    public HeightInput() {}
    
    // Constructor
    public HeightInput(Double value, HeightUnit unit) {
        this.value = value;
        this.unit = unit;
    }
    
    // Convert to centimeters for storage
    public Double toCentimeters() {
        if (value == null || unit == null) {
            return null;
        }
        
        switch (unit) {
            case CM:
                return value;
            case FEET:
                // Convert feet to centimeters (1 foot = 30.48 cm)
                return value * 30.48;
            default:
                return null;
        }
    }
    
    // Convert from centimeters to the specified unit
    public static HeightInput fromCentimeters(Double cmValue, HeightUnit targetUnit) {
        if (cmValue == null) {
            return null;
        }
        
        switch (targetUnit) {
            case CM:
                return new HeightInput(cmValue, HeightUnit.CM);
            case FEET:
                // Convert centimeters to feet
                return new HeightInput(cmValue / 30.48, HeightUnit.FEET);
            default:
                return null;
        }
    }
    
    // Validation method
    @JsonIgnore
    public boolean isValid() {
        if (value == null || unit == null) {
            return false;
        }
        
        switch (unit) {
            case CM:
                return value >= 100 && value <= 250; // 100-250 cm
            case FEET:
                return value >= 3.28 && value <= 8.20; // ~3.28-8.20 feet (100-250 cm)
            default:
                return false;
        }
    }
    
    // Get validation error message
    @JsonIgnore
    public String getValidationErrorMessage() {
        if (value == null || unit == null) {
            return "Height value and unit are required";
        }
        
        switch (unit) {
            case CM:
                if (value < 100 || value > 250) {
                    return "Height must be between 100 and 250 cm";
                }
                break;
            case FEET:
                if (value < 3.28 || value > 8.20) {
                    return "Height must be between 3.28 and 8.20 feet";
                }
                break;
        }
        
        return null;
    }
    
    // Getters and Setters
    public Double getValue() {
        return value;
    }
    
    public void setValue(Double value) {
        this.value = value;
    }
    
    public HeightUnit getUnit() {
        return unit;
    }
    
    public void setUnit(HeightUnit unit) {
        this.unit = unit;
    }
} 