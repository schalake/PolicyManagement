package com.eana.insurance.controller;

import com.eana.insurance.request.PolicyRequestDto;
import com.eana.insurance.response.PolicyResponseDto;
import com.eana.insurance.service.PolicyService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


@RestController
@RequestMapping("/insurance/api")
public class PolicyController {

    private static final Logger logger = LoggerFactory.getLogger(PolicyController.class);

    @Autowired
    private PolicyService policyService;

    @PostMapping("/policies")
    public ResponseEntity<?> createPolicy(@RequestBody @Valid PolicyRequestDto policyRequestDto){
        logger.info("Received request to create policy: {}", policyRequestDto);
        try {
            PolicyResponseDto savedPolicyResponse = policyService.createPolicy(policyRequestDto);
            logger.info("Policy created with number: {}", savedPolicyResponse.getPolicyNumber());

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Policy created successfully.");
            response.put("data", savedPolicyResponse);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            logger.error("Error while creating policy: ", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/policies/{policyNumber}")
    public ResponseEntity<?> getStudentById(@PathVariable String policyNumber){
        logger.info("Fetching policy with number: {}", policyNumber);
        try {
            Optional<PolicyResponseDto> policyData = policyService.getPolicy(policyNumber);
            if (policyData.isEmpty()) {
                logger.warn("Policy not found for number: {}", policyNumber);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Policy does not exist.");
            }

            logger.info("Policy found: {}", policyData.get());
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Policy retrieved successfully.");
            response.put("data", policyData.get());

            //return new ResponseEntity<>(policyData.get(), HttpStatus.OK);
            return ResponseEntity.status(HttpStatus.OK).body(response);

        } catch (Exception e) {
            logger.error("Error while retrieving policy: ", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}