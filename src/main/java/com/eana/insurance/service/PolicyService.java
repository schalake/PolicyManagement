package com.eana.insurance.service;

import com.eana.insurance.request.PolicyRequestDto;
import com.eana.insurance.response.PolicyResponseDto;

import java.util.Optional;

public interface PolicyService {
    PolicyResponseDto createPolicy(PolicyRequestDto policyRequestDto) throws Exception;
    Optional<PolicyResponseDto> getPolicy(String policyNumber);
}
