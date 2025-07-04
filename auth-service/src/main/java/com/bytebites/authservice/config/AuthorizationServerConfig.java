package com.bytebites.authservice.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
// Removed unused imports from the previous attempt if they were introduced
// import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
// import org.springframework.security.web.util.matcher.OrRequestMatcher;
// import org.springframework.security.web.util.matcher.RequestMatcher; // No longer needed if using getEndpointsMatcher directly


import java.security.KeyPair;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.UUID;

import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;

@Configuration
public class AuthorizationServerConfig {

    @Value("${jwt.rsa.private-key}")
    private String privateKeyEncoded;

    @Value("${jwt.rsa.public-key}")
    private String publicKeyEncoded;

    @Bean
    @Order(1) // Ensure this filter chain is processed first
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http)
            throws Exception {
        OAuth2AuthorizationServerConfigurer authorizationServerConfigurer =
                new OAuth2AuthorizationServerConfigurer();

        // Apply the Authorization Server configuration first, so its matchers are available
        http.apply(authorizationServerConfigurer);

        // Configure general HTTP security for the Authorization Server endpoints
        // This ensures this filter chain only applies to OAuth2 specific paths.
        http
                .securityMatcher(authorizationServerConfigurer.getEndpointsMatcher()) // <-- CORRECTED: Use the provided matcher directly
                .authorizeHttpRequests(authorize ->
                        authorize
                                // Publicly accessible OAuth2/OIDC discovery and JWKS endpoints
                                .requestMatchers(
                                        authorizationServerConfigurer.getEndpointsMatcher() // Still using it here for permitAll within its scope
                                ).permitAll()
                                .anyRequest().authenticated() // All other requests within THIS filter chain (i.e., AS endpoints) require authentication
                )
                .csrf(Customizer.withDefaults()) // CSRF for Authorization Server endpoints is usually enabled
                .exceptionHandling(exceptions ->
                        // Redirect unauthenticated users to a login page for authorization requests
                        exceptions.authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login"))
                );
        // .oauth2ResourceServer(oauth2ResourceServer -> // This line was problematic and should not be here.
        //    oauth2ResourceServer.jwt(Customizer.withDefaults())); // Authorization Server is for ISSUING tokens, not consuming its own tokens as a resource server.

        // Configure specific OAuth2 endpoint behaviors if needed (defaults are often sufficient)
        authorizationServerConfigurer
                .tokenEndpoint(Customizer.withDefaults())
                .clientAuthentication(Customizer.withDefaults())
                .authorizationEndpoint(Customizer.withDefaults())
                .oidc(Customizer.withDefaults()); // Enable OIDC support

        return http.build();
    }

    // New Bean: Provides an in-memory RegisteredClientRepository to satisfy Authorization Server dependency
    @Bean
    public RegisteredClientRepository registeredClientRepository() {
        RegisteredClient registeredClient = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId("bytebites-client")
                .clientSecret("{noop}secret") // For development: {noop} is plain text. In production, use BCrypt or similar.
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .redirectUri("http://127.0.0.1:8080/authorized")
                .redirectUri("http://127.0.0.1:8080/login/oauth2/code/bytebites-client-oidc")
                .scope(OidcScopes.OPENID)
                .scope(OidcScopes.PROFILE)
                .scope("message.read")
                .scope("message.write")
                .clientSettings(ClientSettings.builder().requireAuthorizationConsent(false).build())
                .build();

        return new InMemoryRegisteredClientRepository(registeredClient);
    }

    // Bean to load RSA KeyPair from application.yml properties
    @Bean
    public KeyPair rsaKeyPair() throws NoSuchAlgorithmException, java.security.spec.InvalidKeySpecException {
        byte[] privateKeyBytes = Base64.getDecoder().decode(privateKeyEncoded);
        byte[] publicKeyBytes = Base64.getDecoder().decode(publicKeyEncoded);

        KeyFactory keyFactory = KeyFactory.getInstance("RSA");

        PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
        RSAPrivateKey privateKey = (RSAPrivateKey) keyFactory.generatePrivate(privateKeySpec);

        X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyBytes);
        RSAPublicKey publicKey = (RSAPublicKey) keyFactory.generatePublic(publicKeySpec);

        return new KeyPair(publicKey, privateKey);
    }

    // Bean to provide JWKSource for the Authorization Server
    @Bean
    public JWKSource<SecurityContext> jwkSource(KeyPair rsaKeyPair) {
        RSAPublicKey publicKey = (RSAPublicKey) rsaKeyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) rsaKeyPair.getPrivate();

        RSAKey rsaKey = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(UUID.randomUUID().toString())
                .build();

        JWKSet jwkSet = new JWKSet(rsaKey);
        return new ImmutableJWKSet<>(jwkSet);
    }

    // Bean for JwtDecoder, primarily for internal use if this service needs to validate its own tokens
    @Bean
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return org.springframework.security.oauth2.jwt.NimbusJwtDecoder.withJwkSetUri(
                authorizationServerSettings().getJwkSetEndpoint()).build();
    }

    // Bean to configure Authorization Server settings, including the JWKS endpoint URI
    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        // Default settings are usually fine. The JWKS endpoint will default to /oauth2/jwks.
        return AuthorizationServerSettings.builder().build();
    }
}