package com.healthapp.dto;

import java.util.List;

public class ActivityLogPaginatedResponse {
    
    private List<ActivityLogResponse> items;
    
    private Integer page;
    
    private Integer limit;
    
    private Long total;
    
    // Constructors
    public ActivityLogPaginatedResponse() {}
    
    public ActivityLogPaginatedResponse(List<ActivityLogResponse> items, Integer page, Integer limit, Long total) {
        this.items = items;
        this.page = page;
        this.limit = limit;
        this.total = total;
    }
    
    // Getters and Setters
    public List<ActivityLogResponse> getItems() { return items; }
    public void setItems(List<ActivityLogResponse> items) { this.items = items; }
    
    public Integer getPage() { return page; }
    public void setPage(Integer page) { this.page = page; }
    
    public Integer getLimit() { return limit; }
    public void setLimit(Integer limit) { this.limit = limit; }
    
    public Long getTotal() { return total; }
    public void setTotal(Long total) { this.total = total; }
}
