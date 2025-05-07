package com.eana.insurance.controller;

import com.eana.insurance.request.PolicyRequestDto;
import com.eana.insurance.response.PolicyResponseDto;
import com.eana.insurance.service.PolicyService;
import jakarta.validation.Valid;
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

    @Autowired
    private PolicyService policyService;

    @PostMapping("/policies")
    public ResponseEntity<?> createPolicy(@RequestBody @Valid PolicyRequestDto policyRequestDto){
        try {
            PolicyResponseDto savedPolicyResponse = policyService.createPolicy(policyRequestDto);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Policy created successfully.");
            response.put("data", savedPolicyResponse);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/policies/{policyNumber}")
    public ResponseEntity<?> getStudentById(@PathVariable String policyNumber){
        try {
            Optional<PolicyResponseDto> policyData = policyService.getPolicy(policyNumber);
            if (policyData.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Policy does not exist.");
            }

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Policy retrieved successfully.");
            response.put("data", policyData.get());

            //return new ResponseEntity<>(policyData.get(), HttpStatus.OK);
            return ResponseEntity.status(HttpStatus.OK).body(response);

        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
