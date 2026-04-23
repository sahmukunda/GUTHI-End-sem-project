package com.guthi;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.guthi.support.MongoSpringBootTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthRegisterGuthiMemberTest extends MongoSpringBootTestSupport {

    @Autowired private MockMvc mockMvc;

    @Test
    void registerGuthiMember_returnsToken() throws Exception {
        mockMvc.perform(
                        post("/api/auth/register-guthi-member")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "fullName": "Trustee Test",
                                          "email": "trustee-guthi-test@example.com",
                                          "password": "password123",
                                          "inviteCode": "test-invite-code"
                                        }
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }
}
