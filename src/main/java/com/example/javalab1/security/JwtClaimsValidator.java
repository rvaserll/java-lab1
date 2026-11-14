package com.example.javalab1.security;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

public class JwtClaimsValidator implements OAuth2TokenValidator<Jwt> {
    private final String audience;
    public JwtClaimsValidator(String audience) { this.audience = audience; }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        if (jwt.getExpiresAt() == null || jwt.getAudience() == null || !jwt.getAudience().contains(audience)) {
            return OAuth2TokenValidatorResult.failure(
                    new OAuth2Error("invalid_token", "Required token claims are missing or invalid", null));
        }
        return OAuth2TokenValidatorResult.success();
    }
}
