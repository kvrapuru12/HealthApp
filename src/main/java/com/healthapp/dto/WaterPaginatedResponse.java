package com.healthapp.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Paginated response for water entries")
public class WaterPaginatedResponse {
    
    @Schema(description = "List of water entries")
    private List<WaterResponse> items;
    
    @Schema(description = "Current page number", example = "1")
    private Integer page;
    
    @Schema(description = "Number of items per page", example = "20")
    private Integer limit;
    
    @Schema(description = "Total number of water entries", example = "6")
    private Long total;
    
    // Constructors
    public WaterPaginatedResponse() {}
    
    public WaterPaginatedResponse(List<WaterResponse> items, Integer page, Integer limit, Long total) {
        this.items = items;
        this.page = page;
        this.limit = limit;
        this.total = total;
    }
    
    // Getters and Setters
    public List<WaterResponse> getItems() { return items; }
    public void setItems(List<WaterResponse> items) { this.items = items; }
    
    public Integer getPage() { return page; }
    public void setPage(Integer page) { this.page = page; }
    
    public Integer getLimit() { return limit; }
    public void setLimit(Integer limit) { this.limit = limit; }
    
    public Long getTotal() { return total; }
    public void setTotal(Long total) { this.total = total; }
}
