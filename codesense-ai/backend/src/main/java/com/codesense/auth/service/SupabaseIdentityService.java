package com.codesense.auth.service;

import com.codesense.common.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class SupabaseIdentityService {

    private final WebClient.Builder webClientBuilder;

    @Value("${supabase.url:}")
    private String supabaseUrl;

    @Value("${supabase.anon-key:}")
    private String supabaseAnonKey;

    public String verifyAccessToken(String accessToken) {
        if (supabaseUrl.isBlank() || supabaseAnonKey.isBlank()) {
            throw new BadRequestException("Social sign-in is not configured.");
        }
        try {
            Map<?, ?> identity = webClientBuilder.build()
                .get()
                .uri(supabaseUrl + "/auth/v1/user")
                .header("apikey", supabaseAnonKey)
                .headers(headers -> headers.setBearerAuth(accessToken))
                .retrieve()
                .bodyToMono(Map.class)
                .block();
            String email = identity == null ? null : String.valueOf(identity.get("email"));
            if (email == null || email.isBlank() || "null".equals(email)) {
                throw new BadRequestException("Social identity did not include an email address.");
            }
            return email;
        } catch (BadRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BadRequestException("Social sign-in could not be verified.");
        }
    }
}