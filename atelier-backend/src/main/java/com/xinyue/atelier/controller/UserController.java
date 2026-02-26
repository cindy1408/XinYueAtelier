package com.xinyue.atelier.controller;

import com.xinyue.atelier.model.User;
import com.xinyue.atelier.repository.UserRepo;
import com.xinyue.atelier.security.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class UserController {

    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private UserRepo userRepo;

    @GetMapping("/me")
    private ResponseEntity<?> getMe(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");

        String email = jwtUtil.getEmail(token);

        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return ResponseEntity.ok(Map.of(
                "id",          user.getId(),
                "email",       user.getEmail(),
                "name",        user.getName(),
                "role",        user.getRole(),
                "createdAt",   user.getCreatedAt()
        ));
    }
}