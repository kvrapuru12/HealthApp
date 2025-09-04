package com.healthapp.service;

import com.healthapp.dto.*;

import com.healthapp.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@Transactional
public class VoiceCycleLogService {
    
    private static final Logger logger = LoggerFactory.getLogger(VoiceCycleLogService.class);
    
    @Autowired
    private MenstrualCycleService menstrualCycleService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired(required = false)
    private AiCycleVoiceParsingService aiCycleVoiceParsingService;
    
    public VoiceCycleLogResponse processVoiceCycleLog(VoiceCycleLogRequest request, Long authenticatedUserId) {
        try {
            // Validate user access
            if (!request.getUserId().equals(authenticatedUserId)) {
                throw new IllegalArgumentException("Users can only create cycle logs for themselves");
            }
            
            // Validate user exists
            userRepository.findById(authenticatedUserId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            
            // Check if AI service is available
            if (aiCycleVoiceParsingService == null) {
                throw new RuntimeException("AI voice parsing service is not available. Please configure OpenAI API key.");
            }
            
            // Parse voice text using AI
            AiCycleVoiceParsingService.ParsedCycleData parsedData = aiCycleVoiceParsingService.parseVoiceText(request.getVoiceText());
            
            // Create cycle entry
            MenstrualCycleCreateRequest cycleRequest = new MenstrualCycleCreateRequest();
            cycleRequest.setUserId(authenticatedUserId);
            cycleRequest.setPeriodStartDate(parsedData.getPeriodStartDate());
            cycleRequest.setCycleLength(parsedData.getCycleLength());
            cycleRequest.setPeriodDuration(parsedData.getPeriodDuration());
            cycleRequest.setIsCycleRegular(parsedData.getIsCycleRegular());
            
            MenstrualCycleCreateResponse cycleResponse = menstrualCycleService.createCycle(cycleRequest, authenticatedUserId);
            
            // Calculate estimated next period
            LocalDate nextPeriod = parsedData.getPeriodStartDate().plusDays(parsedData.getCycleLength());
            
            logger.info("Created cycle log from voice input: {} for user: {}", cycleResponse.getId(), authenticatedUserId);
            
            return new VoiceCycleLogResponse("Cycle logged", parsedData.getPeriodStartDate(), nextPeriod);
            
        } catch (Exception e) {
            logger.error("Error processing voice cycle log: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process voice input: " + e.getMessage());
        }
    }
}
