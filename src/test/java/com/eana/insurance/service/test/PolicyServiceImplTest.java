package com.eana.insurance.service.test;

import com.eana.insurance.aws.AWSLocalStackServices;
import com.eana.insurance.entity.Policy;
import com.eana.insurance.mapper.PolicyMapper;
import com.eana.insurance.repository.PolicyRepository;
import com.eana.insurance.request.PolicyRequestDto;
import com.eana.insurance.service.PolicyServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest
public class PolicyServiceImplTest {

    @InjectMocks
    private PolicyServiceImpl policyService;

    @Mock
    private PolicyRepository policyRepository;

    @Mock
    private AWSLocalStackServices awsLocalStackServices;

    private PolicyRequestDto newPolicyRequestDto;
    private PolicyRequestDto savedPolicyRequestDto;
    private String policyNumber;

    @BeforeEach
    void setUp(){
        MockitoAnnotations.openMocks(this);
        policyNumber = "POLABCD123";
        newPolicyRequestDto = PolicyRequestDto.builder().firstName("FName")
                .lastName("LName").policyType("auto").startDate(LocalDate.parse("2025-05-01"))
                .endDate(LocalDate.parse("2026-05-01")).build();

        savedPolicyRequestDto =  PolicyRequestDto.builder().policyNumber(policyNumber).firstName("FName")
                .lastName("LName").policyType("auto").startDate(LocalDate.parse("2025-05-01"))
                .endDate(LocalDate.parse("2026-05-01")).build();

    }

    @Test
    public void createPolicyTest() throws Exception {

        Policy newPolicy = PolicyMapper.INSTANCE.policyRequestDtoToPolicy(newPolicyRequestDto);
        Policy savedPolicy = PolicyMapper.INSTANCE.policyRequestDtoToPolicy(savedPolicyRequestDto);
        savedPolicy.setId(1L); // Mock setting an ID (e.g., auto-generated ID)

        Mockito.when(policyRepository.save(Mockito.any(Policy.class))).thenReturn(savedPolicy);
        // Mock the behavior of AWSLocalStackServices (publishing to SNS and saving to DynamoDB)
        doNothing().when(awsLocalStackServices).publishToSNSANDSaveToDynamo(anyString());

        policyService.createPolicy(newPolicyRequestDto);

        assertEquals("FName", savedPolicy.getFirstName());
        assertEquals("LName", savedPolicy.getLastName());
        verify(awsLocalStackServices, times(1)).publishToSNSANDSaveToDynamo(anyString());
    }

    @Test
    public void getPolicyTest() {
        Policy savedPolicy = PolicyMapper.INSTANCE.policyRequestDtoToPolicy(savedPolicyRequestDto);
        Mockito.when(policyRepository.findByPolicyNumber(policyNumber)).thenReturn(Optional.of(savedPolicy));
        policyService.getPolicy(policyNumber);

        assertEquals(policyNumber, savedPolicy.getPolicyNumber());
        Mockito.verify(policyRepository,Mockito.times(1)).findByPolicyNumber(policyNumber);
    }

}
