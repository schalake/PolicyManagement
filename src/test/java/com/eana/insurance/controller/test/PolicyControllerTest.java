package com.eana.insurance.controller.test;

import com.eana.insurance.controller.PolicyController;
import com.eana.insurance.request.PolicyRequestDto;
import com.eana.insurance.response.PolicyResponseDto;
import com.eana.insurance.service.PolicyServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

//@SpringBootTest
//@AutoConfigureMockMvc
@WebMvcTest(PolicyController.class)
class PolicyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    //@InjectMocks
    //private PolicyController policyController;

    @MockBean
    private PolicyServiceImpl policyService;

    @Autowired
    private ObjectMapper objectMapper;

    private PolicyRequestDto requestDto;
    private PolicyResponseDto responseDto;

    @BeforeEach
    void setUp() {
        //MockitoAnnotations.openMocks(this);
        requestDto = PolicyRequestDto.builder()
                .policyNumber("POL1234567")
                .firstName("FName")
                .lastName("LName")
                .policyType("auto")
                .startDate(LocalDate.of(2025, 1, 1))
                .endDate(LocalDate.of(2026, 1, 1))
                .build();

        responseDto = new PolicyResponseDto();
        responseDto.setPolicyNumber("POL1234567");
        responseDto.setFirstName("FName");
        responseDto.setLastName("LName");
        responseDto.setPolicyType("auto");
        responseDto.setStartDate(LocalDate.of(2025, 1, 1));
        responseDto.setEndDate(LocalDate.of(2026, 1, 1));
    }

    @Test
    void testCreatePolicy_success() throws Exception {
        Mockito.when(policyService.createPolicy(any(PolicyRequestDto.class))).thenReturn(responseDto);

        mockMvc.perform(post("/insurance/api/policies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Policy created successfully."))
                .andExpect(jsonPath("$.data.policyNumber").value("POL1234567"))
                .andExpect(jsonPath("$.data.firstName").value("FName"));
    }

    @Test
    void testCreatePolicy_validationError() throws Exception {
        PolicyRequestDto invalidRequest = PolicyRequestDto.builder()
                .firstName("")  // Invalid: @NotEmpty
                .lastName("")   // Invalid: @NotEmpty
                .build();

        mockMvc.perform(post("/insurance/api/policies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").exists());
    }

    @Test
    void testGetPolicyByNumber_found() throws Exception {
        Mockito.when(policyService.getPolicy("POL1234567")).thenReturn(Optional.of(responseDto));

        mockMvc.perform(get("/insurance/api/policies/POL1234567"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Policy retrieved successfully."))
                .andExpect(jsonPath("$.data.policyNumber").value("POL1234567"));
    }

    @Test
    void testGetPolicyByNumber_notFound() throws Exception {
        Mockito.when(policyService.getPolicy("INVALID123")).thenReturn(Optional.empty());

        mockMvc.perform(get("/insurance/api/policies/INVALID123"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Policy does not exist."));
    }
}