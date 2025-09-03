package com.healthapp.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public class FoodItemPaginatedResponse {
    
    @Schema(description = "List of food items")
    private List<FoodItemResponse> foodItems;
    
    @Schema(description = "Current page number")
    private Integer currentPage;
    
    @Schema(description = "Total number of pages")
    private Integer totalPages;
    
    @Schema(description = "Total number of food items")
    private Long totalItems;
    
    @Schema(description = "Number of items per page")
    private Integer itemsPerPage;
    
    @Schema(description = "Whether there is a next page")
    private Boolean hasNext;
    
    @Schema(description = "Whether there is a previous page")
    private Boolean hasPrevious;
    
    // Constructors
    public FoodItemPaginatedResponse() {}
    
    public FoodItemPaginatedResponse(List<FoodItemResponse> foodItems, Integer currentPage, 
                                   Integer totalPages, Long totalItems, Integer itemsPerPage,
                                   Boolean hasNext, Boolean hasPrevious) {
        this.foodItems = foodItems;
        this.currentPage = currentPage;
        this.totalPages = totalPages;
        this.totalItems = totalItems;
        this.itemsPerPage = itemsPerPage;
        this.hasNext = hasNext;
        this.hasPrevious = hasPrevious;
    }
    
    public FoodItemPaginatedResponse(List<FoodItemResponse> foodItems, Integer currentPage, 
                                   Integer limit, Long totalItems) {
        this.foodItems = foodItems;
        this.currentPage = currentPage;
        this.totalPages = (int) Math.ceil((double) totalItems / limit);
        this.totalItems = totalItems;
        this.itemsPerPage = limit;
        this.hasNext = currentPage < this.totalPages;
        this.hasPrevious = currentPage > 1;
    }
    
    // Getters and Setters
    public List<FoodItemResponse> getFoodItems() {
        return foodItems;
    }
    
    public void setFoodItems(List<FoodItemResponse> foodItems) {
        this.foodItems = foodItems;
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
}
