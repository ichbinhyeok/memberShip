package org.example.membership.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class BadgeControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void updateBadgeStates_ok() throws Exception {
        String body = "{\"id\":1,\"name\":\"User1\"}";
        mockMvc.perform(post("/badges/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    @Test
    void updateBadgeStates_badRequest() throws Exception {
        String body = "invalid";
        mockMvc.perform(post("/badges/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void activate_ok() throws Exception {
        String body = "{\"userId\":1,\"categoryId\":1,\"active\":true}";
        mockMvc.perform(post("/badges/activate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    @Test
    void activate_badRequest() throws Exception {
        String body = "{\"userId\":\"x\"}";
        mockMvc.perform(post("/badges/activate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}
