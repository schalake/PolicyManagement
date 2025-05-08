package com.eana.insurance.service;

import com.eana.insurance.aws.AWSLocalStackServices;
import com.eana.insurance.entity.Policy;
import com.eana.insurance.mapper.PolicyMapper;
import com.eana.insurance.repository.PolicyRepository;
import com.eana.insurance.request.PolicyRequestDto;
import com.eana.insurance.response.PolicyResponseDto;
import com.eana.insurance.utility.JSONUtil;
import com.eana.insurance.utility.PolicyNumberGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class PolicyServiceImpl implements PolicyService {

    private static final String DYNAMO_TABLE = "PolicyTable";
    private static final String QUEUE_NAME = "PolicyQueue";
    private static final String TOPIC_NAME = "PolicyTopic";

    @Autowired
    PolicyRepository policyRepository;

    @Autowired
    AWSLocalStackServices awsLocalStackServices;

    @Override
    public PolicyResponseDto createPolicy(PolicyRequestDto policyRequestDto) throws Exception {
        String policyNumber = PolicyNumberGenerator.generatePolicyNumber();
        policyRequestDto.setPolicyNumber(policyNumber);
        Policy policy = PolicyMapper.INSTANCE.policyRequestDtoToPolicy(policyRequestDto);
        Policy savedPolicy = policyRepository.save(policy);

        String requestJSONString = JSONUtil.createPolicyRequestJson(policyRequestDto);
        // Send JSON string message to SNS -> SQS -> Dynamo DB.
        awsLocalStackServices.publishToSNSANDSaveToDynamo(requestJSONString);

        return PolicyMapper.INSTANCE.policyToPolicyResponseDto(savedPolicy);
    }

    @Override
    public Optional<PolicyResponseDto> getPolicy(String policyNumber) {
        return policyRepository.findByPolicyNumber(policyNumber).map(PolicyMapper.INSTANCE::policyToPolicyResponseDto);
    }
}
