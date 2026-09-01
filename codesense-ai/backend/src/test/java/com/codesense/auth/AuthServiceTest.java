package com.codesense.auth;

import com.codesense.auth.dto.LoginRequest;
import com.codesense.auth.dto.RegisterRequest;
import com.codesense.auth.model.User;
import com.codesense.auth.repository.UserRepository;
import com.codesense.auth.security.JwtService;
import com.codesense.auth.service.AuthService;
import com.codesense.common.exception.ConflictException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock JwtService jwtService;
    @Mock PasswordEncoder passwordEncoder;
    @Mock AuthenticationManager authenticationManager;

    @InjectMocks AuthService authService;

    @Test
    void register_success() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("test@example.com");
        req.setPassword("password123");

        when(passwordEncoder.encode(anyString())).thenReturn("$2a$hashed");
        when(jwtService.generateToken(any())).thenReturn("mock.jwt.token");
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = authService.register(req);

        assertThat(response.getToken()).isEqualTo("mock.jwt.token");
        assertThat(response.getEmail()).isEqualTo("test@example.com");
    }

    @Test
    void register_duplicateEmail_throws() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("existing@example.com");
        req.setPassword("password123");
        req.setName("User");

        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(req))
            .isInstanceOf(ConflictException.class)
            .hasMessage("An account with this email already exists.");
    }

    @Test
    void login_success() {
        LoginRequest req = new LoginRequest();
        req.setEmail("test@example.com");
        req.setPassword("password123");

        User mockUser = User.builder()
            .email("test@example.com")
            .name("Test")
            .password("$2a$hashed")
            .build();

        when(jwtService.generateToken(any())).thenReturn("mock.jwt.token");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(mockUser));

        var response = authService.login(req);
        assertThat(response.getToken()).isNotBlank();
    }

    @Test
    void register_withoutName_usesEmailPrefixAndHashesPassword() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail(" User@Example.com ");
        req.setPassword("password123");

        when(passwordEncoder.encode("password123")).thenReturn("$2a$hashed");
        when(jwtService.generateToken(any())).thenReturn("token");
        when(userRepository.existsByEmail("user@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = authService.register(req);

        assertThat(response.getEmail()).isEqualTo("user@example.com");
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(argThat(user ->
            user.getName().equals("user") && user.getPassword().equals("$2a$hashed")));
    }

    @Test
    void login_invalidCredentialsPropagatesAuthenticationFailure() {
        LoginRequest req = new LoginRequest();
        req.setEmail("user@example.com");
        req.setPassword("wrong");
        doThrow(new BadCredentialsException("bad credentials"))
            .when(authenticationManager).authenticate(any());

        assertThatThrownBy(() -> authService.login(req))
            .isInstanceOf(BadCredentialsException.class);
        verifyNoInteractions(jwtService);
    }
}
