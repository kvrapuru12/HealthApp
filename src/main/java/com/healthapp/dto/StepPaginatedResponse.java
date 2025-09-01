package com.healthapp.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Paginated response for step entries")
public class StepPaginatedResponse {
    
    @Schema(description = "List of step entries for the current page")
    private List<StepResponse> items;
    
    @Schema(description = "Current page number", example = "1")
    private Integer page;
    
    @Schema(description = "Number of items per page", example = "20")
    private Integer limit;
    
    @Schema(description = "Total number of step entries across all pages", example = "12")
    private Long total;
    
    // Constructors
    public StepPaginatedResponse() {}
    
    public StepPaginatedResponse(List<StepResponse> items, Integer page, Integer limit, Long total) {
        this.items = items;
        this.page = page;
        this.limit = limit;
        this.total = total;
    }
    
    // Getters and Setters
    public List<StepResponse> getItems() { return items; }
    public void setItems(List<StepResponse> items) { this.items = items; }
    
    public Integer getPage() { return page; }
    public void setPage(Integer page) { this.page = page; }
    
    public Integer getLimit() { return limit; }
    public void setLimit(Integer limit) { this.limit = limit; }
    
    public Long getTotal() { return total; }
    public void setTotal(Long total) { this.total = total; }
}
