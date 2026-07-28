package com.xinyue.atelier.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigCorsTest {

    private SecurityConfig securityConfig;

    @BeforeEach
    void setUp() {
        securityConfig = new SecurityConfig();
        ReflectionTestUtils.setField(securityConfig, "frontendUrl", "http://localhost:5173");
    }

    @Test
    void corsConfigurationAllowsExpectedOrigin() {
        CorsConfigurationSource source = securityConfig.corsConfigurationSource();

        CorsConfiguration config = source.getCorsConfiguration(new MockHttpServletRequest("GET", "/api/test"));

        assertThat(config).isNotNull();
        assertThat(config.getAllowedOrigins()).containsExactly("http://localhost:5173");
    }

    @Test
    void corsConfigurationAllowsExpectedMethods() {
        CorsConfigurationSource source = securityConfig.corsConfigurationSource();
        CorsConfiguration config = source.getCorsConfiguration(new MockHttpServletRequest("GET", "/api/test"));

        assertThat(config.getAllowedMethods())
                .containsExactlyInAnyOrder("GET", "POST", "PUT", "DELETE", "OPTIONS");
    }

    @Test
    void corsConfigurationAllowsAllHeaders() {
        CorsConfigurationSource source = securityConfig.corsConfigurationSource();
        CorsConfiguration config = source.getCorsConfiguration(new MockHttpServletRequest("GET", "/api/test"));

        assertThat(config.getAllowedHeaders()).containsExactly("*");
    }

    @Test
    void corsConfigurationAllowsCredentials() {
        CorsConfigurationSource source = securityConfig.corsConfigurationSource();
        CorsConfiguration config = source.getCorsConfiguration(new MockHttpServletRequest("GET", "/api/test"));

        assertThat(config.getAllowCredentials()).isTrue();
    }
}