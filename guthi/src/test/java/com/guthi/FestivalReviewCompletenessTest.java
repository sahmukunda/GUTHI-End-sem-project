package com.guthi;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guthi.support.MongoSpringBootTestSupport;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FestivalReviewCompletenessTest extends MongoSpringBootTestSupport {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void reviewRejectedUntilFestivalMarkedComplete() throws Exception {
        String id = UUID.randomUUID().toString().substring(0, 8);
        String adminEmail = "fest-admin-" + id + "@example.com";
        String userEmail = "fest-user-" + id + "@example.com";
        String password = "password123";

        mockMvc.perform(
                        post("/api/auth/register-guthi-member")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "fullName": "Fest Admin",
                                          "email": "%s",
                                          "password": "%s",
                                          "inviteCode": "test-invite-code"
                                        }
                                        """
                                                .formatted(adminEmail, password)))
                .andExpect(status().isOk());

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

        mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        { "fullName": "Fest User", "email": "%s", "password": "%s" }
                                        """
                                                .formatted(userEmail, password)))
                .andExpect(status().isOk());

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

        LocalDate d = LocalDate.now().plusDays(7);
        MvcResult createFest =
                mockMvc.perform(
                                post("/api/festivals")
                                        .header("Authorization", "Bearer " + adminToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                """
                                                { "name": "Test Tihar", "description": "Lights", "festivalDate": "%s" }
                                                """
                                                        .formatted(d)))
                        .andExpect(status().isOk())
                        .andReturn();
        String festId =
                objectMapper.readTree(createFest.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(
                        post("/api/festivals/" + festId + "/reviews")
                                .header("Authorization", "Bearer " + userToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        { "rating": 5, "comment": "Great" }
                                        """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(
                        put("/api/admin/festivals/" + festId + "/complete")
                                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(
                        post("/api/festivals/" + festId + "/reviews")
                                .header("Authorization", "Bearer " + userToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        { "rating": 5, "comment": "Great" }
                                        """))
                .andExpect(status().isOk());
    }
}
