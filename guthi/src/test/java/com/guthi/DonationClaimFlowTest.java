package com.guthi;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guthi.enums.Role;
import com.guthi.repository.UserRepository;
import com.guthi.support.IssuedAccounts;
import com.guthi.support.MongoSpringBootTestSupport;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DonationClaimFlowTest extends MongoSpringBootTestSupport {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @Test
    void submitClaimThenAdminApprove_updatesMemberSummary() throws Exception {
        String id = UUID.randomUUID().toString().substring(0, 8);
        String userEmail = "donor-" + id + "@example.com";
        String adminEmail = "guthi-" + id + "@example.com";
        String password = "password123";
        String txnId = String.format("%012d", Math.abs(UUID.randomUUID().getMostSignificantBits()) % 1_000_000_000_000L);

        IssuedAccounts.issue(userRepository, passwordEncoder, "Pool Donor", userEmail, password, Role.USER);

        MvcResult userLogin =
                mockMvc.perform(
                                post("/api/auth/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                """
                                                { "email": "%s", "password": "%s" }
                                                """
                                                        .formatted(userEmail, password)))
                        .andExpect(status().isOk())
                        .andReturn();
        String userToken =
                objectMapper.readTree(userLogin.getResponse().getContentAsString()).get("token").asText();

        mockMvc.perform(
                        post("/api/donations/claims")
                                .header("Authorization", "Bearer " + userToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        { "amount": 1000, "note": "test", "upiTransactionId": "%s" }
                                        """
                                                .formatted(txnId)))
                .andExpect(status().isOk());

        mockMvc.perform(
                        get("/api/donations/summary")
                                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.myPendingClaimsTotal").value(1000))
                .andExpect(jsonPath("$.myContributionsTotal").value(0));

        IssuedAccounts.issue(userRepository, passwordEncoder, "Trustee Claim Test", adminEmail, password, Role.ADMIN);

        MvcResult adminLogin =
                mockMvc.perform(
                                post("/api/auth/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                """
                                                { "email": "%s", "password": "%s" }
                                                """
                                                        .formatted(adminEmail, password)))
                        .andExpect(status().isOk())
                        .andReturn();
        String adminToken =
                objectMapper.readTree(adminLogin.getResponse().getContentAsString()).get("token").asText();

        MvcResult listResult =
                mockMvc.perform(
                                get("/api/admin/donation-claims")
                                        .header("Authorization", "Bearer " + adminToken))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$[0].upiTransactionId").value(txnId))
                        .andReturn();

        JsonNode claims = objectMapper.readTree(listResult.getResponse().getContentAsString());
        String claimId = claims.get(0).get("id").asText();

        mockMvc.perform(
                        put("/api/admin/donation-claims/" + claimId + "/approve")
                                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(
                        get("/api/donations/summary")
                                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.myContributionsTotal").value(1000))
                .andExpect(jsonPath("$.myPendingClaimsTotal").value(0));
    }
}
