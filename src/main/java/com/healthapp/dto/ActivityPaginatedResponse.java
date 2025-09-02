package com.healthapp.dto;

import java.util.List;

public class ActivityPaginatedResponse {
    
    private List<ActivityResponse> items;
    
    private Integer page;
    
    private Integer limit;
    
    private Long total;
    
    // Constructors
    public ActivityPaginatedResponse() {}
    
    public ActivityPaginatedResponse(List<ActivityResponse> items, Integer page, Integer limit, Long total) {
        this.items = items;
        this.page = page;
        this.limit = limit;
        this.total = total;
    }
    
    // Getters and Setters
    public List<ActivityResponse> getItems() { return items; }
    public void setItems(List<ActivityResponse> items) { this.items = items; }
    
    public Integer getPage() { return page; }
    public void setPage(Integer page) { this.page = page; }
    
    public Integer getLimit() { return limit; }
    public void setLimit(Integer limit) { this.limit = limit; }
    
    public Long getTotal() { return total; }
    public void setTotal(Long total) { this.total = total; }
}
