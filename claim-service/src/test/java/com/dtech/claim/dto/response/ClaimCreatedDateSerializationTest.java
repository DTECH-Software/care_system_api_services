package com.dtech.claim.dto.response;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClaimCreatedDateSerializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void medicalClaimCreationTimeIncludesSriLankaOffset() throws Exception {
        InsuranceClaimRequestResponseDTO claim = new InsuranceClaimRequestResponseDTO();
        claim.setCreatedDate(Date.from(Instant.parse("2026-09-11T16:56:30Z")));

        assertEquals("2026-09-11T22:26:30+05:30",
                objectMapper.readTree(objectMapper.writeValueAsString(claim)).get("createdDate").asText());
    }

    @Test
    void deathClaimCreationTimeIncludesSriLankaOffset() throws Exception {
        DeathClaimRequestResponseDTO claim = new DeathClaimRequestResponseDTO();
        claim.setCreatedDate(Date.from(Instant.parse("2026-09-11T16:56:30Z")));

        assertEquals("2026-09-11T22:26:30+05:30",
                objectMapper.readTree(objectMapper.writeValueAsString(claim)).get("createdDate").asText());
    }
}
