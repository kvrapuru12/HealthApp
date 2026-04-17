package com.healthapp.controller;

import com.healthapp.dto.applehealth.AppleHealthIngestRequest;
import com.healthapp.dto.applehealth.AppleHealthIngestResponse;
import com.healthapp.service.AppleHealthIngestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/integrations/apple-health")
@Tag(name = "Apple Health", description = "Ingest HealthKit-backed samples (MVP: steps)")
@CrossOrigin(origins = "*")
public class AppleHealthIntegrationController {

    private static final Logger logger = LoggerFactory.getLogger(AppleHealthIntegrationController.class);

    private final AppleHealthIngestService appleHealthIngestService;

    public AppleHealthIntegrationController(AppleHealthIngestService appleHealthIngestService) {
        this.appleHealthIngestService = appleHealthIngestService;
    }

    @PostMapping("/ingest")
    @Operation(summary = "Ingest Apple Health step samples", description = "Idempotent upsert by externalSampleId (schema v1, STEPS only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Batch processed (per-sample status in body)"),
            @ApiResponse(responseCode = "400", description = "Invalid body or unsupported schema version"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<AppleHealthIngestResponse> ingest(@Valid @RequestBody AppleHealthIngestRequest request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Long userId = (Long) authentication.getPrincipal();
            AppleHealthIngestResponse body = appleHealthIngestService.ingest(userId, request);
            return ResponseEntity.ok(body);
        } catch (IllegalArgumentException e) {
            logger.warn("Apple Health ingest validation failed: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Apple Health ingest failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
