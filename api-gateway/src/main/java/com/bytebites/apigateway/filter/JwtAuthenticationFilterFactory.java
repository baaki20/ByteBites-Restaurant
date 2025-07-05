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
        try {
            JWKSource<SecurityContext> jwkSource = new RemoteJWKSet<>(new URL(jwkSetUri));

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

            List<String> authHeaders = exchange.getRequest().getHeaders().get(AUTHORIZATION_HEADER);

            if (authHeaders == null || authHeaders.isEmpty() || !authHeaders.get(0).startsWith(BEARER_PREFIX)) {
                return onError(exchange, "Missing or invalid Authorization header", HttpStatus.UNAUTHORIZED);
            }

            String token = authHeaders.get(0).substring(BEARER_PREFIX.length());

            try {
                JWTClaimsSet claimsSet = jwtProcessor.process(token, null);

                ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                        .header("X-Auth-User", claimsSet.getSubject())
                        .header("X-Auth-Roles", String.join(",", (List<String>) claimsSet.getClaim("roles")))
                        .build();

                return chain.filter(exchange.mutate().request(mutatedRequest).build());

            } catch (BadJOSEException e) {
                System.err.println("JWT Validation Error (BadJOSEException): " + e.getMessage());
                return onError(exchange, "Invalid JWT Token: " + e.getMessage(), HttpStatus.UNAUTHORIZED);
            } catch (Exception e) {
                System.err.println("Error processing JWT Token: " + e.getMessage());
                return onError(exchange, "Error processing JWT Token", HttpStatus.INTERNAL_SERVER_ERROR);
            }
        };
    }

    private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus httpStatus) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(httpStatus);
        return response.setComplete();
    }
}