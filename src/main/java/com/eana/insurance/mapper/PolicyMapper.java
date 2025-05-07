package com.eana.insurance.mapper;

import com.eana.insurance.entity.Policy;
import com.eana.insurance.request.PolicyRequestDto;
import com.eana.insurance.response.PolicyResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import java.util.List;
import java.util.Optional;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PolicyMapper {
    PolicyMapper INSTANCE = Mappers.getMapper(PolicyMapper.class);

    PolicyRequestDto policyToPolicyRequestDto(Policy policy);
    Policy policyRequestDtoToPolicy(PolicyRequestDto policyRequestDto);

    PolicyResponseDto policyToPolicyResponseDto(Policy policy);
    Policy policyResponseDtoToPolicy(PolicyResponseDto policyResponseDto);

}
