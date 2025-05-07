package com.eana.insurance.service;

import com.eana.insurance.entity.Policy;
import com.eana.insurance.mapper.PolicyMapper;
import com.eana.insurance.repository.PolicyRepository;
import com.eana.insurance.request.PolicyRequestDto;
import com.eana.insurance.response.PolicyResponseDto;
import com.eana.insurance.utility.PolicyNumberGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class PolicyServiceImpl implements PolicyService {

    @Autowired
    PolicyRepository policyRepository;

    @Override
    public PolicyResponseDto createPolicy(PolicyRequestDto policyRequestDto) {
        String policyNumber = PolicyNumberGenerator.generatePolicyNumber();
        policyRequestDto.setPolicyNumber(policyNumber);
        Policy policy = PolicyMapper.INSTANCE.policyRequestDtoToPolicy(policyRequestDto);
        Policy savedPolicy = policyRepository.save(policy);
        return PolicyMapper.INSTANCE.policyToPolicyResponseDto(savedPolicy);
    }

    @Override
    public Optional<PolicyResponseDto> getPolicy(String policyNumber) {
        //Optional<Policy> policy =  policyRepository.findByPolicyNumber(policyNumber);
        return policyRepository.findByPolicyNumber(policyNumber).map(PolicyMapper.INSTANCE::policyToPolicyResponseDto);
        //return PolicyMapper.INSTANCE.policyToPolicyResponseDtos(policy);
    }
}
