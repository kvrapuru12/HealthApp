package com.healthapp.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Paginated response for menstrual cycles")
public class MenstrualCyclePaginatedResponse {
    
    @Schema(description = "List of menstrual cycles")
    private List<MenstrualCycleResponse> cycles;
    
    @Schema(description = "Current page number", example = "1")
    private Integer currentPage;
    
    @Schema(description = "Number of items per page", example = "20")
    private Integer limit;
    
    @Schema(description = "Total number of cycles", example = "50")
    private Long totalCycles;
    
    @Schema(description = "Total number of pages", example = "3")
    private Integer totalPages;
    
    // Constructors
    public MenstrualCyclePaginatedResponse() {}
    
    public MenstrualCyclePaginatedResponse(List<MenstrualCycleResponse> cycles, Integer currentPage, 
                                         Integer limit, Long totalCycles) {
        this.cycles = cycles;
        this.currentPage = currentPage;
        this.limit = limit;
        this.totalCycles = totalCycles;
        this.totalPages = (int) Math.ceil((double) totalCycles / limit);
    }
    
    // Getters and Setters
    public List<MenstrualCycleResponse> getCycles() {
        return cycles;
    }
    
    public void setCycles(List<MenstrualCycleResponse> cycles) {
        this.cycles = cycles;
    }
    
    public Integer getCurrentPage() {
        return currentPage;
    }
    
    public void setCurrentPage(Integer currentPage) {
        this.currentPage = currentPage;
    }
    
    public Integer getLimit() {
        return limit;
    }
    
    public void setLimit(Integer limit) {
        this.limit = limit;
    }
    
    public Long getTotalCycles() {
        return totalCycles;
    }
    
    public void setTotalCycles(Long totalCycles) {
        this.totalCycles = totalCycles;
    }
    
    public Integer getTotalPages() {
        return totalPages;
    }
    
    public void setTotalPages(Integer totalPages) {
        this.totalPages = totalPages;
    }
}
