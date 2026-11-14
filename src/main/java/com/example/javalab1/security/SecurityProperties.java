package com.example.javalab1.security;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "market.security")
public record SecurityProperties(@NotBlank String apiKey, @NotBlank String issuer,
                                 @NotBlank String jwkSetUri, @NotBlank String audience) {}
