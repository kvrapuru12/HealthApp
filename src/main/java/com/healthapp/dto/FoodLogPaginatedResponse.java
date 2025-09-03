package com.healthapp.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public class FoodLogPaginatedResponse {
    
    @Schema(description = "List of food logs")
    private List<FoodLogResponse> foodLogs;
    
    @Schema(description = "Current page number")
    private Integer currentPage;
    
    @Schema(description = "Total number of pages")
    private Integer totalPages;
    
    @Schema(description = "Total number of food logs")
    private Long totalItems;
    
    @Schema(description = "Number of items per page")
    private Integer itemsPerPage;
    
    @Schema(description = "Whether there is a next page")
    private Boolean hasNext;
    
    @Schema(description = "Whether there is a previous page")
    private Boolean hasPrevious;
    
    @Schema(description = "Total calories for the filtered results")
    private Double totalCalories;
    
    @Schema(description = "Total protein for the filtered results")
    private Double totalProtein;
    
    @Schema(description = "Total carbs for the filtered results")
    private Double totalCarbs;
    
    @Schema(description = "Total fat for the filtered results")
    private Double totalFat;
    
    @Schema(description = "Total fiber for the filtered results")
    private Double totalFiber;
    
    // Constructors
    public FoodLogPaginatedResponse() {}
    
    public FoodLogPaginatedResponse(List<FoodLogResponse> foodLogs, Integer currentPage, 
                                   Integer totalPages, Long totalItems, Integer itemsPerPage,
                                   Boolean hasNext, Boolean hasPrevious, Double totalCalories,
                                   Double totalProtein, Double totalCarbs, Double totalFat, Double totalFiber) {
        this.foodLogs = foodLogs;
        this.currentPage = currentPage;
        this.totalPages = totalPages;
        this.totalItems = totalItems;
        this.itemsPerPage = itemsPerPage;
        this.hasNext = hasNext;
        this.hasPrevious = hasPrevious;
        this.totalCalories = totalCalories;
        this.totalProtein = totalProtein;
        this.totalCarbs = totalCarbs;
        this.totalFat = totalFat;
        this.totalFiber = totalFiber;
    }
    
    public FoodLogPaginatedResponse(List<FoodLogResponse> foodLogs, Integer currentPage, 
                                   Integer limit, Long totalItems, Double totalCalories,
                                   Double totalProtein, Double totalCarbs, Double totalFat, Double totalFiber) {
        this.foodLogs = foodLogs;
        this.currentPage = currentPage;
        this.totalPages = (int) Math.ceil((double) totalItems / limit);
        this.totalItems = totalItems;
        this.itemsPerPage = limit;
        this.hasNext = currentPage < this.totalPages;
        this.hasPrevious = currentPage > 1;
        this.totalCalories = totalCalories;
        this.totalProtein = totalProtein;
        this.totalCarbs = totalCarbs;
        this.totalFat = totalFat;
        this.totalFiber = totalFiber;
    }
    
    // Getters and Setters
    public List<FoodLogResponse> getFoodLogs() {
        return foodLogs;
    }
    
    public void setFoodLogs(List<FoodLogResponse> foodLogs) {
        this.foodLogs = foodLogs;
    }
    
    public Integer getCurrentPage() {
        return currentPage;
    }
    
    public void setCurrentPage(Integer currentPage) {
        this.currentPage = currentPage;
    }
    
    public Integer getTotalPages() {
        return totalPages;
    }
    
    public void setTotalPages(Integer totalPages) {
        this.totalPages = totalPages;
    }
    
    public Long getTotalItems() {
        return totalItems;
    }
    
    public void setTotalItems(Long totalItems) {
        this.totalItems = totalItems;
    }
    
    public Integer getItemsPerPage() {
        return itemsPerPage;
    }
    
    public void setItemsPerPage(Integer itemsPerPage) {
        this.itemsPerPage = itemsPerPage;
    }
    
    public Boolean getHasNext() {
        return hasNext;
    }
    
    public void setHasNext(Boolean hasNext) {
        this.hasNext = hasNext;
    }
    
    public Boolean getHasPrevious() {
        return hasPrevious;
    }
    
    public void setHasPrevious(Boolean hasPrevious) {
        this.hasPrevious = hasPrevious;
    }
    
    public Double getTotalCalories() {
        return totalCalories;
    }
    
    public void setTotalCalories(Double totalCalories) {
        this.totalCalories = totalCalories;
    }
    
    public Double getTotalProtein() {
        return totalProtein;
    }
    
    public void setTotalProtein(Double totalProtein) {
        this.totalProtein = totalProtein;
    }
    
    public Double getTotalCarbs() {
        return totalCarbs;
    }
    
    public void setTotalCarbs(Double totalCarbs) {
        this.totalCarbs = totalCarbs;
    }
    
    public Double getTotalFat() {
        return totalFat;
    }
    
    public void setTotalFat(Double totalFat) {
        this.totalFat = totalFat;
    }
    
    public Double getTotalFiber() {
        return totalFiber;
    }
    
    public void setTotalFiber(Double totalFiber) {
        this.totalFiber = totalFiber;
    }
}
