package com.xinyue.atelier.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/data/**").permitAll()
                        .requestMatchers("/patterns/download/**").permitAll()
                        .anyRequest().permitAll() // TEMP while developing
                )
                .csrf(csrf -> csrf.disable());

        return http.build();
    }
}
