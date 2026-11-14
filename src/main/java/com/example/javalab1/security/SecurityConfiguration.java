package com.example.javalab1.security;

import jakarta.servlet.DispatcherType;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(SecurityProperties.class)
public class SecurityConfiguration {
    @Bean
    JwtDecoder jwtDecoder(SecurityProperties properties) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(properties.jwkSetUri())
                .jwsAlgorithm(SignatureAlgorithm.RS256).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(properties.issuer()),
                new JwtClaimsValidator(properties.audience())));
        return decoder;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, SecurityProperties properties,
                                            SecurityProblemHandler problems) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/products", "/api/products/**")
                            .hasAuthority("SCOPE_products:read")
                        .requestMatchers(HttpMethod.HEAD, "/api/products", "/api/products/**")
                            .hasAuthority("SCOPE_products:read")
                        .requestMatchers(HttpMethod.POST, "/api/products").hasAuthority("SCOPE_products:write")
                        .requestMatchers(HttpMethod.PUT, "/api/products/*").hasAuthority("SCOPE_products:write")
                        .requestMatchers(HttpMethod.DELETE, "/api/products/*").hasAuthority("SCOPE_products:write")
                        .anyRequest().denyAll())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(problems).accessDeniedHandler(problems))
                .oauth2ResourceServer(resource -> resource
                        .jwt(jwt -> {}).authenticationEntryPoint(problems).accessDeniedHandler(problems))
                // Deliberately not a servlet Filter bean: execute exactly once inside Security.
                .addFilterBefore(new ApiKeyFilter(properties.apiKey(), problems), BearerTokenAuthenticationFilter.class);
        return http.build();
    }
}
