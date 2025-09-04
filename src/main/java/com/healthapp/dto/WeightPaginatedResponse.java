package com.healthapp.dto;



import java.util.List;

public class WeightPaginatedResponse {
    
    private List<WeightResponse> items;
    
    private Integer page;
    
    private Integer limit;
    
    private Long total;
    
    // Constructors
    public WeightPaginatedResponse() {}
    
    public WeightPaginatedResponse(List<WeightResponse> items, Integer page, Integer limit, Long total) {
        this.items = items;
        this.page = page;
        this.limit = limit;
        this.total = total;
    }
    
    // Getters and Setters
    public List<WeightResponse> getItems() { return items; }
    public void setItems(List<WeightResponse> items) { this.items = items; }
    
    public Integer getPage() { return page; }
    public void setPage(Integer page) { this.page = page; }
    
    public Integer getLimit() { return limit; }
    public void setLimit(Integer limit) { this.limit = limit; }
    
    public Long getTotal() { return total; }
    public void setTotal(Long total) { this.total = total; }
}
