package com.eana.insurance.utility;

import com.eana.insurance.request.PolicyRequestDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class JSONUtil {

    public static String createPolicyRequestJson(PolicyRequestDto policyRequestDto) throws Exception {
        // Create ObjectMapper to convert Java objects to JSON
        ObjectMapper mapper = new ObjectMapper();

        // Create a JavaTimeModule to support java.time classes like LocalDate
        JavaTimeModule module = new JavaTimeModule();

        // Add custom serializer for LocalDate to format as dd-MM-yyyy
        module.addSerializer(LocalDate.class, new LocalDateSerializer(DateTimeFormatter.ofPattern("dd-MM-yyyy")));

        // Register the module with ObjectMapper
        mapper.registerModule(module);

        // Disable writing dates as timestamps (by default it writes them as long values)
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Convert the policyRequestDto to a JSON string
        return mapper.writeValueAsString(policyRequestDto);
    }
}
