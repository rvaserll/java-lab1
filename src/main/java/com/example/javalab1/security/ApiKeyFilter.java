package com.example.javalab1.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.List;

public class ApiKeyFilter extends OncePerRequestFilter {
    public static final String HEADER = "X-API-Key";
    private final byte[] expectedKey;
    private final SecurityProblemHandler problems;

    public ApiKeyFilter(String expectedKey, SecurityProblemHandler problems) {
        this.expectedKey = expectedKey.getBytes(StandardCharsets.UTF_8);
        this.problems = problems;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        List<String> keys = Collections.list(request.getHeaders(HEADER));
        if (keys.isEmpty()) {
            chain.doFilter(request, response);
            return;
        }
        // Choose one authentication method per request; never let one credential
        // conceal a bad credential of the other type.
        boolean ambiguous = request.getHeader(HttpHeaders.AUTHORIZATION) != null || keys.size() != 1;
        if (ambiguous || keys.get(0).isBlank()
                || !MessageDigest.isEqual(expectedKey, keys.get(0).getBytes(StandardCharsets.UTF_8))) {
            SecurityContextHolder.clearContext();
            problems.commence(request, response, new BadCredentialsException("Invalid API credentials"));
            return;
        }
        var authentication = UsernamePasswordAuthenticationToken.authenticated("api-key-client", null,
                List.of(new SimpleGrantedAuthority("SCOPE_products:read"),
                        new SimpleGrantedAuthority("SCOPE_products:write")));
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        chain.doFilter(request, response);
    }
}
