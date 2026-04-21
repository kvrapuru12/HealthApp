package com.healthapp.controller;

import com.healthapp.dto.applehealth.DashboardDailyResponse;
import com.healthapp.service.DashboardDailyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/dashboard")
@Tag(name = "Dashboard", description = "Combined daily read models")
@CrossOrigin(origins = "*")
public class DashboardController {

    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);

    private final DashboardDailyService dashboardDailyService;

    public DashboardController(DashboardDailyService dashboardDailyService) {
        this.dashboardDailyService = dashboardDailyService;
    }

    @GetMapping("/daily")
    @Operation(summary = "Daily dashboard", description = "Merged steps and sleep from Apple Health and manual entries for one local calendar day")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Dashboard payload"),
            @ApiResponse(responseCode = "400", description = "Invalid query parameters"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<DashboardDailyResponse> getDaily(
            @RequestParam("localDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate localDate,
            @RequestParam("timeZone") String timeZone) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Long userId = (Long) authentication.getPrincipal();
            DashboardDailyResponse body = dashboardDailyService.getDaily(userId, localDate, timeZone);
            return ResponseEntity.ok(body);
        } catch (IllegalArgumentException e) {
            logger.warn("Dashboard daily validation failed: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Dashboard daily failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
