package com.guthi;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guthi.enums.Role;
import com.guthi.repository.UserRepository;
import com.guthi.support.IssuedAccounts;
import com.guthi.support.MongoSpringBootTestSupport;
import java.time.LocalDate;
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
class FestivalReviewCompletenessTest extends MongoSpringBootTestSupport {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @Test
    void reviewRejectedUntilFestivalMarkedComplete() throws Exception {
        String id = UUID.randomUUID().toString().substring(0, 8);
        String adminEmail = "fest-admin-" + id + "@example.com";
        String userEmail = "fest-user-" + id + "@example.com";
        String password = "password123";

        IssuedAccounts.issue(userRepository, passwordEncoder, "Fest Admin", adminEmail, password, Role.ADMIN);

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

        IssuedAccounts.issue(userRepository, passwordEncoder, "Fest User", userEmail, password, Role.USER);

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
