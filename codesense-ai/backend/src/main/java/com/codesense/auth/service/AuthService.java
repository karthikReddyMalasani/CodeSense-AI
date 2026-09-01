package com.codesense.auth.service;

import com.codesense.auth.dto.*;
import com.codesense.auth.model.Role;
import com.codesense.auth.model.User;
import com.codesense.auth.repository.UserRepository;
import com.codesense.auth.security.JwtService;
import com.codesense.common.exception.BadRequestException;
import com.codesense.common.exception.ConflictException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Set;

@Slf4j
@Service
public class AuthService implements UserDetailsService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final SupabaseIdentityService supabaseIdentityService;

    @Autowired
    public AuthService(UserRepository userRepository,
                       JwtService jwtService,
                       PasswordEncoder passwordEncoder,
                       @Lazy AuthenticationManager authenticationManager,
                       SupabaseIdentityService supabaseIdentityService) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.supabaseIdentityService = supabaseIdentityService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ConflictException("An account with this email already exists.");
        }

        User user = User.builder()
            .name(defaultName(request.getName(), normalizedEmail))
            .email(normalizedEmail)
            .password(passwordEncoder.encode(request.getPassword()))
            .role(Role.USER)
            .build();

        user = userRepository.save(user);
        log.info("New user registered: {}", user.getEmail());

        String token = jwtService.generateToken(user);
        return buildAuthResponse(token, user);
    }

    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(normalizedEmail, request.getPassword())
        );

        User user = userRepository.findByEmail(normalizedEmail)
            .orElseThrow(() -> new UsernameNotFoundException("Invalid email or password."));

        String token = jwtService.generateToken(user);
        log.info("User logged in: {}", user.getEmail());
        return buildAuthResponse(token, user);
    }

    @Transactional
    public AuthResponse socialLogin(SocialLoginRequest request) {
        if (!Set.of("google", "github").contains(request.getProvider().toLowerCase(Locale.ROOT))) {
            throw new BadRequestException("Unsupported social sign-in provider.");
        }
        String verifiedEmail = supabaseIdentityService.verifyAccessToken(request.getAccessToken());
        String normalizedEmail = normalizeEmail(verifiedEmail);
        User user = userRepository.findByEmail(normalizedEmail)
            .orElseGet(() -> {
                log.info("Creating new user via social login ({}) for email: {}", request.getProvider(), normalizedEmail);
                User newUser = User.builder()
                    .name(request.getName())
                    .email(normalizedEmail)
                    .password(passwordEncoder.encode("SOCIAL_AUTH_" + java.util.UUID.randomUUID()))
                    .role(Role.USER)
                    .build();
                return userRepository.save(newUser);
            });

        String token = jwtService.generateToken(user);
        log.info("User authenticated via social login ({}): {}", request.getProvider(), user.getEmail());
        return buildAuthResponse(token, user);
    }

    public UserProfileDto getProfile(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
        return UserProfileDto.builder()
            .id(user.getId())
            .name(user.getName())
            .email(user.getEmail())
            .role(user.getRole().name())
            .createdAt(user.getCreatedAt())
            .build();
    }

    /**
     * Verify legacy account credentials and return migration info.
     * This allows old accounts to be migrated to Supabase on first login.
     */
    public LegacyMigrationResponse verifyLegacyCredentials(LoginRequest request) {
        String normalizedEmail = request.getEmail().toLowerCase();
        User user = userRepository.findByEmail(normalizedEmail)
            .orElse(null);

        if (user == null) {
            log.warn("Legacy login attempt for non-existent user: {}", normalizedEmail);
            throw new UsernameNotFoundException("Account not found in the system");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Legacy login failed - invalid password for: {}", normalizedEmail);
            throw new BadRequestException("Invalid credentials");
        }

        log.info("Legacy credentials verified for: {}", normalizedEmail);
        return LegacyMigrationResponse.builder()
            .email(user.getEmail())
            .name(user.getName())
            .message("Please use this account to sign in with Supabase. We're migrating your account.")
            .requiresSupabaseCreation(true)
            .build();
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(normalizeEmail(email))
            .orElseThrow(() -> new UsernameNotFoundException("Invalid email or password."));
    }

    private AuthResponse buildAuthResponse(String token, User user) {
        return AuthResponse.builder()
            .token(token)
            .tokenType("Bearer")
            .userId(user.getId())
            .name(user.getName())
            .email(user.getEmail())
            .role(user.getRole().name())
            .build();
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private String defaultName(String name, String email) {
        if (name != null && !name.isBlank()) return name.trim();
        return email.substring(0, email.indexOf('@'));
    }
}
