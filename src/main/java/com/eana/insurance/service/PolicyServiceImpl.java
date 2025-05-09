package com.eana.insurance.service;

import com.eana.insurance.aws.AWSLocalStackServices;
import com.eana.insurance.entity.Policy;
import com.eana.insurance.mapper.PolicyMapper;
import com.eana.insurance.repository.PolicyRepository;
import com.eana.insurance.request.PolicyRequestDto;
import com.eana.insurance.response.PolicyResponseDto;
import com.eana.insurance.utility.JSONUtil;
import com.eana.insurance.utility.PolicyNumberGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class PolicyServiceImpl implements PolicyService {

    private static final Logger logger = LoggerFactory.getLogger(PolicyServiceImpl.class);

    private static final String DYNAMO_TABLE = "PolicyTable";
    private static final String QUEUE_NAME = "PolicyQueue";
    private static final String TOPIC_NAME = "PolicyTopic";

    @Autowired
    PolicyRepository policyRepository;

    @Autowired
    AWSLocalStackServices awsLocalStackServices;

    @Override
    public PolicyResponseDto createPolicy(PolicyRequestDto policyRequestDto) throws Exception {
        logger.info("Generating policy number...");
        String policyNumber = PolicyNumberGenerator.generatePolicyNumber();
        policyRequestDto.setPolicyNumber(policyNumber);
        logger.debug("Generated policy number: {}", policyNumber);

        Policy policy = PolicyMapper.INSTANCE.policyRequestDtoToPolicy(policyRequestDto);
        Policy savedPolicy = policyRepository.save(policy);
        logger.info("Policy saved with ID: {}", savedPolicy.getId());

        String requestJSONString = JSONUtil.createPolicyRequestJson(policyRequestDto);
        logger.debug("Serialized JSON string: {}", requestJSONString);

        // Send JSON string message to SNS -> SQS -> Dynamo DB.
        logger.info("Publishing message to AWS SNS via LocalStack...");
        awsLocalStackServices.publishToSNSANDSaveToDynamo(requestJSONString);
        logger.info("Message published and saved to DynamoDB.");

        return PolicyMapper.INSTANCE.policyToPolicyResponseDto(savedPolicy);
    }

    @Override
    public Optional<PolicyResponseDto> getPolicy(String policyNumber) {
        logger.info("Fetching policy with number: {}", policyNumber);
        return policyRepository.findByPolicyNumber(policyNumber).map(PolicyMapper.INSTANCE::policyToPolicyResponseDto);
    }
}