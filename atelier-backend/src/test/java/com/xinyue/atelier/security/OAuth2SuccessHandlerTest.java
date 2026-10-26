package com.xinyue.atelier.security;

import com.xinyue.atelier.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OAuth2SuccessHandlerTest {

    @Mock
    private UserService userService;

    @Mock
    private Authentication authentication;

    @Mock
    private OAuth2User oauthUser;

    private OAuth2SuccessHandler successHandler;

    @BeforeEach
    void setUp() {
        successHandler = new OAuth2SuccessHandler(
                userService, "http://localhost:5173", "owner@example.com");
        when(authentication.getPrincipal()).thenReturn(oauthUser);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void onAuthenticationSuccess_createsUserForAllowedEmail() throws Exception {
        when(oauthUser.getAttribute("email")).thenReturn("OWNER@example.com");
        when(oauthUser.getAttribute("sub")).thenReturn("google-user-id");
        when(oauthUser.getAttribute("name")).thenReturn("Owner");
        MockHttpServletResponse response = new MockHttpServletResponse();

        successHandler.onAuthenticationSuccess(new MockHttpServletRequest(), response, authentication);

        verify(userService).findOrCreateUser("google-user-id", "OWNER@example.com", "Owner");
        assertThat(response.getRedirectedUrl()).isEqualTo("http://localhost:5173");
    }

    @Test
    void onAuthenticationSuccess_rejectsOtherEmailAndInvalidatesSession() throws Exception {
        when(oauthUser.getAttribute("email")).thenReturn("someone-else@example.com");
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpSession session = (MockHttpSession) request.getSession(true);
        MockHttpServletResponse response = new MockHttpServletResponse();

        successHandler.onAuthenticationSuccess(request, response, authentication);

        verifyNoInteractions(userService);
        assertThat(session.isInvalid()).isTrue();
        assertThat(response.getRedirectedUrl())
                .isEqualTo("http://localhost:5173/login?error=not_allowed");
    }

    @Test
    void onAuthenticationSuccess_rejectsMissingEmail() throws Exception {
        when(oauthUser.getAttribute("email")).thenReturn(null);
        MockHttpServletResponse response = new MockHttpServletResponse();

        successHandler.onAuthenticationSuccess(new MockHttpServletRequest(), response, authentication);

        verifyNoInteractions(userService);
        assertThat(response.getRedirectedUrl())
                .isEqualTo("http://localhost:5173/login?error=not_allowed");
    }
}
