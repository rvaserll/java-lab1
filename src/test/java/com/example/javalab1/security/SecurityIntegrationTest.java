package com.example.javalab1.security;

import com.example.javalab1.support.PostgresTestConfiguration;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestConfiguration.class)
@Transactional
class SecurityIntegrationTest {
    private static final String API = "/api/products";
    private static final String API_KEY = "test-only-api-key-not-for-production";
    private static final RSAKey SIGNING_KEY = newKey();
    private static final RSAKey OTHER_KEY = newKey();
    private static final HttpServer JWKS = startJwks();
    private static final String ISSUER = "http://127.0.0.1:" + JWKS.getAddress().getPort();
    private static final String BODY = """
            {"name":"Authenticated toy","price":12.50,
             "categoryId":"11111111-1111-1111-1111-111111111111","stock":1}
            """;
    @Autowired MockMvc mvc;

    private static RSAKey newKey() {
        try {
            var generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            var pair = generator.generateKeyPair();
            return new RSAKey.Builder((RSAPublicKey) pair.getPublic())
                    .privateKey((RSAPrivateKey) pair.getPrivate()).keyID("test-key").build();
        } catch (Exception exception) { throw new IllegalStateException(exception); }
    }

    private static HttpServer startJwks() {
        try {
            var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/.well-known/jwks.json", exchange -> {
                byte[] body = new JWKSet(SIGNING_KEY.toPublicJWK()).toString().getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                try (var output = exchange.getResponseBody()) { output.write(body); }
            });
            server.start();
            return server;
        } catch (Exception exception) { throw new IllegalStateException(exception); }
    }

    @DynamicPropertySource
    static void securityProperties(DynamicPropertyRegistry registry) {
        registry.add("market.security.issuer", () -> ISSUER);
        registry.add("market.security.jwk-set-uri", () -> ISSUER + "/.well-known/jwks.json");
    }
    @AfterAll
    static void stopJwks() { JWKS.stop(0); }

    private String token(String scope) throws Exception {
        return token(scope, ISSUER, List.of("cosmo-cats-api"), Date.from(Instant.now().plusSeconds(600)),
                Date.from(Instant.now().minusSeconds(5)), SIGNING_KEY);
    }
    private String token(String scope, String issuer, List<String> audience, Date expiration,
                         Date notBefore, RSAKey key) throws Exception {
        var claims = new JWTClaimsSet.Builder().issuer(issuer).subject("student")
                .issueTime(Date.from(Instant.now().minusSeconds(10))).expirationTime(expiration)
                .notBeforeTime(notBefore).claim("scope", scope);
        if (audience != null) claims.audience(audience);
        var jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256)
                .type(JOSEObjectType.JWT).keyID("test-key").build(), claims.build());
        jwt.sign(new RSASSASigner(key));
        return jwt.serialize();
    }

    @Test
    void noCredentialsReturnsJson401WithoutLoginRedirectOrSession() throws Exception {
        mvc.perform(get(API)).andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.instance").value(API))
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(header().doesNotExist("Location"))
                .andExpect(cookie().doesNotExist("JSESSIONID"));
    }

    @Test
    void validApiKeyAllowsReadingAndWritingWithoutBearerToken() throws Exception {
        mvc.perform(get(API).header(ApiKeyFilter.HEADER, API_KEY)).andExpect(status().isOk());
        mvc.perform(post(API).header(ApiKeyFilter.HEADER, API_KEY)
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.name").value("Authenticated toy"))
                .andExpect(cookie().doesNotExist("JSESSIONID"));
    }

    @Test
    void apiKeyAuthenticationDoesNotLeakIntoTheNextRequest() throws Exception {
        mvc.perform(get(API).header(ApiKeyFilter.HEADER, API_KEY)).andExpect(status().isOk());
        mvc.perform(get(API)).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "incorrect-key"})
    void invalidApiKeyReturns401WithoutRevealingExpectedKey(String key) throws Exception {
        mvc.perform(get(API).header(ApiKeyFilter.HEADER, key))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(not(containsString(API_KEY))));
    }

    @Test
    void duplicateApiKeyHeadersAreRejected() throws Exception {
        mvc.perform(get(API).header(ApiKeyFilter.HEADER, API_KEY, API_KEY)).andExpect(status().isUnauthorized());
    }

    @Test
    void simultaneousAuthenticationMethodsAreRejectedEvenWhenBothAreValid() throws Exception {
        mvc.perform(get(API).header(ApiKeyFilter.HEADER, API_KEY)
                        .header("Authorization", "Bearer " + token("products:read")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validApiKeyDoesNotConcealInvalidBearerToken() throws Exception {
        mvc.perform(get(API).header(ApiKeyFilter.HEADER, API_KEY).header("Authorization", "Bearer invalid"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validBearerTokenDoesNotConcealInvalidApiKey() throws Exception {
        mvc.perform(get(API).header(ApiKeyFilter.HEADER, "incorrect")
                        .header("Authorization", "Bearer " + token("products:read")))
                .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @ValueSource(strings = {"Bearer invalid", "Bearer", "Basic abc", "Bearer a.b.c"})
    void malformedOrUnsupportedAuthorizationReturns401(String header) throws Exception {
        mvc.perform(get(API).header("Authorization", header)).andExpect(status().isUnauthorized());
    }

    @Test
    void signedJwtIsVerifiedUsingPublicKeysFetchedFromJwks() throws Exception {
        mvc.perform(get(API).header("Authorization", "Bearer " + token("products:read")))
                .andExpect(status().isOk()).andExpect(cookie().doesNotExist("JSESSIONID"));
    }

    @Test
    void readOnlyTokenCannotCreateProduct() throws Exception {
        mvc.perform(post(API).header("Authorization", "Bearer " + token("products:read"))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.title").value("Access denied"));
    }

    @Test
    void writeTokenAllowsCreatingProduct() throws Exception {
        mvc.perform(post(API).header("Authorization", "Bearer " + token("products:write"))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated());
    }

    @Test
    void writeOnlyTokenCannotReadProductList() throws Exception {
        mvc.perform(get(API).header("Authorization", "Bearer " + token("products:write")))
                .andExpect(status().isForbidden());
    }

    @Test
    void tokenWithoutRequiredScopeIsForbidden() throws Exception {
        mvc.perform(get(API).header("Authorization", "Bearer " + token("unrelated:scope")))
                .andExpect(status().isForbidden());
    }

    @Test
    void expiredTokenReturns401() throws Exception {
        String jwt = token("products:read", ISSUER, List.of("cosmo-cats-api"),
                Date.from(Instant.now().minusSeconds(120)), Date.from(Instant.now().minusSeconds(300)), SIGNING_KEY);
        mvc.perform(get(API).header("Authorization", "Bearer " + jwt)).andExpect(status().isUnauthorized());
    }

    @Test
    void tokenNotYetValidReturns401() throws Exception {
        String jwt = token("products:read", ISSUER, List.of("cosmo-cats-api"),
                Date.from(Instant.now().plusSeconds(600)), Date.from(Instant.now().plusSeconds(300)), SIGNING_KEY);
        mvc.perform(get(API).header("Authorization", "Bearer " + jwt)).andExpect(status().isUnauthorized());
    }

    @Test
    void wrongIssuerReturns401() throws Exception {
        String jwt = token("products:read", "https://untrusted.example", List.of("cosmo-cats-api"),
                Date.from(Instant.now().plusSeconds(600)), null, SIGNING_KEY);
        mvc.perform(get(API).header("Authorization", "Bearer " + jwt)).andExpect(status().isUnauthorized());
    }

    @Test
    void wrongAudienceReturns401() throws Exception {
        String jwt = token("products:read", ISSUER, List.of("other-api"),
                Date.from(Instant.now().plusSeconds(600)), null, SIGNING_KEY);
        mvc.perform(get(API).header("Authorization", "Bearer " + jwt)).andExpect(status().isUnauthorized());
    }

    @Test
    void missingAudienceReturns401InsteadOfServerError() throws Exception {
        String jwt = token("products:read", ISSUER, null, Date.from(Instant.now().plusSeconds(600)), null, SIGNING_KEY);
        mvc.perform(get(API).header("Authorization", "Bearer " + jwt)).andExpect(status().isUnauthorized());
    }

    @Test
    void tokenWithoutExpiryIsRejected() throws Exception {
        String jwt = token("products:read", ISSUER, List.of("cosmo-cats-api"), null, null, SIGNING_KEY);
        mvc.perform(get(API).header("Authorization", "Bearer " + jwt)).andExpect(status().isUnauthorized());
    }

    @Test
    void signatureFromDifferentPrivateKeyIsRejected() throws Exception {
        String jwt = token("products:read", ISSUER, List.of("cosmo-cats-api"),
                Date.from(Instant.now().plusSeconds(600)), null, OTHER_KEY);
        mvc.perform(get(API).header("Authorization", "Bearer " + jwt)).andExpect(status().isUnauthorized());
    }

    @Test
    void unsignedTokenIsRejected() throws Exception {
        String jwt = new PlainJWT(new JWTClaimsSet.Builder().issuer(ISSUER)
                .audience("cosmo-cats-api").claim("scope", "products:read")
                .expirationTime(Date.from(Instant.now().plusSeconds(600))).build()).serialize();
        mvc.perform(get(API).header("Authorization", "Bearer " + jwt)).andExpect(status().isUnauthorized());
    }

    @Test
    void mockJwtCanExerciseAuthorizationIndependentlyOfSignatureValidation() throws Exception {
        mvc.perform(get(API).with(jwt().jwt(value -> value.claim("scope", "products:read"))))
                .andExpect(status().isOk());
    }

    @Test
    void bearerTokenInQueryStringIsNotAccepted() throws Exception {
        mvc.perform(get(API).param("access_token", token("products:read"))).andExpect(status().isUnauthorized());
    }

    @Test
    void headRequestUsesReadPermission() throws Exception {
        mvc.perform(head(API).header("Authorization", "Bearer " + token("products:read")))
                .andExpect(status().isOk());
    }

    @Test
    void unconfiguredRoutesAreDenied() throws Exception {
        mvc.perform(get("/unconfigured").header(ApiKeyFilter.HEADER, API_KEY)).andExpect(status().isForbidden());
    }
}
