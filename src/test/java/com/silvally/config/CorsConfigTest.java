package com.silvally.config;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CorsConfigTest {

    private static final String FRONT = "https://front.example.test";

    private static MockHttpServletRequestBuilder createPlayer() {
        return post("/api/v1/players")
                .header("Origin", FRONT)
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"cors\"}");
    }

    @Nested
    @SpringBootTest(properties = "silvally.cors.allowed-origins=" + FRONT + ", https://other.example.test")
    @AutoConfigureMockMvc
    class AllowedOrigins {

        @Autowired MockMvc mockMvc;

        @Test
        void preflightAllowsConfiguredOriginWithCommandHeaders() throws Exception {
            mockMvc.perform(options("/api/v1/players/{id}/turns/advance", UUID.randomUUID())
                            .header("Origin", FRONT)
                            .header("Access-Control-Request-Method", "POST")
                            .header("Access-Control-Request-Headers", "Content-Type, Idempotency-Key, X-Expected-Version"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Access-Control-Allow-Origin", FRONT))
                    .andExpect(header().string("Access-Control-Allow-Headers", "Content-Type, Idempotency-Key, X-Expected-Version"));
        }

        @Test
        void actualResponseExposesLocationToConfiguredOrigin() throws Exception {
            mockMvc.perform(createPlayer())
                    .andExpect(status().isCreated())
                    .andExpect(header().exists("Location"))
                    .andExpect(header().string("Access-Control-Allow-Origin", FRONT))
                    .andExpect(header().string("Access-Control-Expose-Headers", "Location"));
        }

        @Test
        void unknownOriginIsRejected() throws Exception {
            mockMvc.perform(options("/api/v1/players")
                            .header("Origin", "https://evil.example.test")
                            .header("Access-Control-Request-Method", "POST"))
                    .andExpect(status().isForbidden())
                    .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
        }
    }

    @Nested
    @SpringBootTest
    @AutoConfigureMockMvc
    class DefaultSameOrigin {

        @Autowired MockMvc mockMvc;

        @Test
        void crossOriginHeadersAreNotSentWithoutConfiguration() throws Exception {
            mockMvc.perform(createPlayer())
                    .andExpect(status().isCreated())
                    .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
        }
    }
}
