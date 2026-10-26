package com.xinyue.atelier.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestConfig.class)
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
class CsrfIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void csrfEndpointReturnsTokenDetails() throws Exception {
        mockMvc.perform(get("/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headerName").value("X-CSRF-TOKEN"))
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void logoutRequiresCsrfToken() throws Exception {
        mockMvc.perform(post("/logout"))
                .andExpect(status().isForbidden());

        MvcResult tokenResult = mockMvc.perform(get("/csrf"))
                .andExpect(status().isOk())
                .andReturn();
        String token = objectMapper.readTree(tokenResult.getResponse().getContentAsString())
                .get("token")
                .asText();
        MockHttpSession session = (MockHttpSession) tokenResult.getRequest().getSession(false);

        mockMvc.perform(post("/logout")
                        .session(session)
                        .header("X-CSRF-TOKEN", token))
                .andExpect(status().isOk());
    }
}
