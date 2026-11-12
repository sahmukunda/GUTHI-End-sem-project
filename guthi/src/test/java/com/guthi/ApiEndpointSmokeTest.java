package com.guthi;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guthi.enums.Role;
import com.guthi.repository.UserRepository;
import com.guthi.support.IssuedAccounts;
import com.guthi.support.MongoSpringBootTestSupport;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

/**
 * End-to-end HTTP checks for every public REST surface and representative authenticated flows.
 * Uses the same Mongo Testcontainer pattern as other integration tests.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiEndpointSmokeTest extends MongoSpringBootTestSupport {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @Test
    void allPrimaryEndpoints_respondAsExpected() throws Exception {
        String id = UUID.randomUUID().toString().substring(0, 8);
        String password = "password123";
        String user1Email = "smoke-u1-" + id + "@example.com";
        String user2Email = "smoke-u2-" + id + "@example.com";
        String adminEmail = "smoke-adm-" + id + "@example.com";

        // --- Anonymous / permitAll ---
        mockMvc.perform(get("/api/donations/summary")).andExpect(status().isForbidden());

        mockMvc.perform(get("/api/donations/qr"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG));

        mockMvc.perform(get("/api/festivals")).andExpect(status().isOk()).andExpect(jsonPath("$").isArray());

        mockMvc.perform(get("/api/volunteer-events")).andExpect(status().isUnauthorized());

        // --- Register & login: community users + admin ---
        IssuedAccounts.issue(userRepository, passwordEncoder, "Smoke User One", user1Email, password, Role.USER);
        IssuedAccounts.issue(userRepository, passwordEncoder, "Smoke User Two", user2Email, password, Role.USER);
        IssuedAccounts.issue(userRepository, passwordEncoder, "Smoke Admin", adminEmail, password, Role.ADMIN);

        mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        { "fullName": "Nobody", "email": "new-%s", "password": "%s" }
                                        """
                                                .formatted(user1Email, password)))
                .andExpect(status().isBadRequest());

        String token1 = loginToken(user1Email, password);
        String token2 = loginToken(user2Email, password);
        String adminToken = loginToken(adminEmail, password);

        mockMvc.perform(
                        post("/api/auth/logout").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        // --- Authenticated reads ---
        mockMvc.perform(get("/api/donations/summary").header("Authorization", "Bearer " + token1))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/volunteer-events").header("Authorization", "Bearer " + token1))
                .andExpect(status().isOk());

        // --- Admin: me, requests list ---
        mockMvc.perform(get("/api/admin/me").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(adminEmail));

        mockMvc.perform(get("/api/requests/all").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        mockMvc.perform(get("/api/admin/donation-claims").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        // --- Festival: admin create, list, mark complete (admin path), user review ---
        LocalDate festDay = LocalDate.now().plusDays(14);
        MvcResult festRes =
                mockMvc.perform(
                                post("/api/festivals")
                                        .header("Authorization", "Bearer " + adminToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                """
                                                { "name": "Smoke Fest", "description": "Test", "festivalDate": "%s" }
                                                """
                                                        .formatted(festDay)))
                        .andExpect(status().isOk())
                        .andReturn();
        String festivalId = objectMapper.readTree(festRes.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(get("/api/festivals/" + festivalId + "/reviews")).andExpect(status().isOk());

        mockMvc.perform(
                        put("/api/admin/festivals/" + festivalId + "/complete")
                                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(
                        post("/api/festivals/" + festivalId + "/reviews")
                                .header("Authorization", "Bearer " + token1)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        { "rating": 4, "comment": "Smoke OK" }
                                        """))
                .andExpect(status().isOk());

        // --- Volunteer: admin create, user signup, admin signups list ---
        String volWhen = LocalDateTime.now().plusDays(20).withNano(0).toString();
        MvcResult volRes =
                mockMvc.perform(
                                post("/api/volunteer-events")
                                        .header("Authorization", "Bearer " + adminToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                """
                                                {
                                                  "title": "Smoke volunteer slot",
                                                  "description": "Help needed",
                                                  "volunteersRequired": 5,
                                                  "eventDate": "%s"
                                                }
                                                """
                                                        .formatted(volWhen)))
                        .andExpect(status().isOk())
                        .andReturn();
        String volEventId = objectMapper.readTree(volRes.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(
                        post("/api/volunteer-events/" + volEventId + "/signup")
                                .header("Authorization", "Bearer " + token1)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        { "participantName": "Volunteer Pat", "mobileNumber": "9801112233" }
                                        """))
                .andExpect(status().isOk());

        mockMvc.perform(
                        get("/api/volunteer-events/" + volEventId + "/signups")
                                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.signups[0].participantName").value("Volunteer Pat"));

        // --- Work posts: create, list, second user accepts ---
        MvcResult postRes =
                mockMvc.perform(
                                post("/api/work-posts")
                                        .header("Authorization", "Bearer " + token1)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                """
                                                {
                                                  "title": "Smoke work offer",
                                                  "description": "Test",
                                                  "mobileNumber": "9810001122"
                                                }
                                                """))
                        .andExpect(status().isOk())
                        .andReturn();
        String workPostId = objectMapper.readTree(postRes.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(get("/api/work-posts").header("Authorization", "Bearer " + token2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(workPostId));

        mockMvc.perform(
                        post("/api/work-posts/" + workPostId + "/accept")
                                .header("Authorization", "Bearer " + token2))
                .andExpect(status().isOk());

        // --- Community request (no fund) + my list + admin approve ---
        MvcResult reqRes =
                mockMvc.perform(
                                post("/api/requests")
                                        .header("Authorization", "Bearer " + token1)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                """
                                                {
                                                  "type": "RITUAL_HELP_PEOPLE",
                                                  "description": "Need volunteers for smoke ritual",
                                                  "peopleRequested": 4
                                                }
                                                """))
                        .andExpect(status().isOk())
                        .andReturn();
        String requestId = objectMapper.readTree(reqRes.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(get("/api/requests/my").header("Authorization", "Bearer " + token1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(requestId));

        mockMvc.perform(
                        put("/api/requests/" + requestId + "/approve")
                                .header("Authorization", "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isOk());

        // --- Donation claim + admin approve (subset of DonationClaimFlowTest) ---
        String txn =
                String.format("%012d", Math.abs(UUID.randomUUID().getMostSignificantBits()) % 1_000_000_000_000L);
        mockMvc.perform(
                        post("/api/donations/claims")
                                .header("Authorization", "Bearer " + token1)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        { "amount": 1000, "note": "smoke", "upiTransactionId": "%s" }
                                        """
                                                .formatted(txn)))
                .andExpect(status().isOk());

        MvcResult claimsList =
                mockMvc.perform(get("/api/admin/donation-claims").header("Authorization", "Bearer " + adminToken))
                        .andExpect(status().isOk())
                        .andReturn();
        JsonNode claims = objectMapper.readTree(claimsList.getResponse().getContentAsString());
        String claimId = null;
        for (JsonNode row : claims) {
            if (txn.equals(row.get("upiTransactionId").asText())) {
                claimId = row.get("id").asText();
                break;
            }
        }
        if (claimId == null) {
            throw new AssertionError("Expected pending claim for txn " + txn);
        }

        mockMvc.perform(
                        put("/api/admin/donation-claims/" + claimId + "/approve")
                                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        // Reject path: second claim then reject
        String txn2 =
                String.format("%012d", Math.abs(UUID.randomUUID().getLeastSignificantBits()) % 1_000_000_000_000L);
        mockMvc.perform(
                        post("/api/donations/claims")
                                .header("Authorization", "Bearer " + token1)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        { "amount": 50, "note": "reject me", "upiTransactionId": "%s" }
                                        """
                                                .formatted(txn2)))
                .andExpect(status().isOk());

        MvcResult claims2 =
                mockMvc.perform(get("/api/admin/donation-claims").header("Authorization", "Bearer " + adminToken))
                        .andExpect(status().isOk())
                        .andReturn();
        String rejectId = null;
        for (JsonNode row : objectMapper.readTree(claims2.getResponse().getContentAsString())) {
            if (txn2.equals(row.get("upiTransactionId").asText())) {
                rejectId = row.get("id").asText();
                break;
            }
        }
        if (rejectId == null) {
            throw new AssertionError("Expected pending claim for txn2 " + txn2);
        }

        mockMvc.perform(
                        put("/api/admin/donation-claims/" + rejectId + "/reject")
                                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    private String loginToken(String email, String password) throws Exception {
        MvcResult r =
                mockMvc.perform(
                                post("/api/auth/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                """
                                                { "email": "%s", "password": "%s" }
                                                """
                                                        .formatted(email, password)))
                        .andExpect(status().isOk())
                        .andReturn();
        return objectMapper.readTree(r.getResponse().getContentAsString()).get("token").asText();
    }
}
