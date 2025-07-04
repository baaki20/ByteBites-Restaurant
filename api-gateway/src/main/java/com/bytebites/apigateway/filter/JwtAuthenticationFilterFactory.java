package com.bytebites.apigateway.filter;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.RemoteJWKSet;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

@Component
public class JwtAuthenticationFilterFactory extends AbstractGatewayFilterFactory<JwtAuthenticationFilterFactory.Config> implements InitializingBean {

    // Inject the JWKS URI from properties
    @Value("${jwt.jwk-set-uri}")
    private String jwkSetUri;

    private ConfigurableJWTProcessor<SecurityContext> jwtProcessor;

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    public JwtAuthenticationFilterFactory() {
        super(Config.class);
    }

    public static class Config {
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        // Initialize the JWT processor once properties are set
        try {
            // JWKSource for remote JWK Set
            JWKSource<SecurityContext> jwkSource = new RemoteJWKSet<>(new URL(jwkSetUri));

            // The JWSKeySelector selects the key(s) for verifying the JWT signature
            JWSKeySelector<SecurityContext> jwsKeySelector =
                    new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, jwkSource);

            DefaultJWTProcessor<SecurityContext> defaultJWTProcessor = new DefaultJWTProcessor<>();
            defaultJWTProcessor.setJWSKeySelector(jwsKeySelector);

            this.jwtProcessor = defaultJWTProcessor;

        } catch (MalformedURLException e) {
            throw new IllegalStateException("Invalid JWK Set URI: " + jwkSetUri, e);
        }
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            // Public paths are handled by API Gateway's SecurityConfig and don't need JWT validation here
            // This filter is applied only to routes that need authentication

            List<String> authHeaders = exchange.getRequest().getHeaders().get(AUTHORIZATION_HEADER);

            if (authHeaders == null || authHeaders.isEmpty() || !authHeaders.get(0).startsWith(BEARER_PREFIX)) {
                return onError(exchange, "Missing or invalid Authorization header", HttpStatus.UNAUTHORIZED);
            }

            String token = authHeaders.get(0).substring(BEARER_PREFIX.length());

            try {
                // Process and validate the JWT token
                JWTClaimsSet claimsSet = jwtProcessor.process(token, null);

                // Add validated user information to request headers for downstream services
                ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                        .header("X-Auth-User", claimsSet.getSubject())
                        // Assuming roles are in a 'roles' claim (from JwtService)
                        .header("X-Auth-Roles", String.join(",", (List<String>) claimsSet.getClaim("roles")))
                        .build();

                return chain.filter(exchange.mutate().request(mutatedRequest).build());

            } catch (BadJOSEException e) {
                // This covers signature validation, invalid claims, etc.
                System.err.println("JWT Validation Error (BadJOSEException): " + e.getMessage());
                return onError(exchange, "Invalid JWT Token: " + e.getMessage(), HttpStatus.UNAUTHORIZED);
            } catch (Exception e) {
                // Catch any other unexpected errors during processing
                System.err.println("Error processing JWT Token: " + e.getMessage());
                return onError(exchange, "Error processing JWT Token", HttpStatus.INTERNAL_SERVER_ERROR); // Use 500 for unexpected errors
            }
        };
    }

    private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus httpStatus) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(httpStatus);
        // Optionally, add a response body for more detail
        // return response.writeWith(Mono.just(response.bufferFactory().wrap(message.getBytes())));
        return response.setComplete();
    }
}