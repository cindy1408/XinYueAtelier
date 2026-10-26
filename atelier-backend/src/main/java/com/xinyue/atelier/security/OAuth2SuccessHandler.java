package com.xinyue.atelier.security;

import com.xinyue.atelier.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Locale;

@Component
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserService userService;
    private final String frontendUrl;
    private final String allowedGoogleEmail;

    public OAuth2SuccessHandler(
            UserService userService,
            @Value("${app.frontend-url}") String frontendUrl,
            @Value("${app.allowed-google-email}") String allowedGoogleEmail) {
        this.userService = userService;
        this.frontendUrl = frontendUrl;
        this.allowedGoogleEmail = allowedGoogleEmail.trim().toLowerCase(Locale.ROOT);
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();
        String email = oauthUser.getAttribute("email");
        if (email == null || !allowedGoogleEmail.equals(email.trim().toLowerCase(Locale.ROOT))) {
            SecurityContextHolder.clearContext();
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }
            response.sendRedirect(frontendUrl + "/login?error=not_allowed");
            return;
        }

        userService.findOrCreateUser(
                oauthUser.getAttribute("sub"),
                email,
                oauthUser.getAttribute("name")
        );

        response.sendRedirect(frontendUrl);
    }
}