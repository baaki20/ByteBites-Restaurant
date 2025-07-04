package com.bytebites.authservice.controller;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.text.ParseException;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

@RestController
public class JwkSetController {

    @Value("${jwt.rsa.public-key}")
    private String publicKeyEncoded;

    private JWKSet jwkSet;

    public JwkSetController(@Value("${jwt.rsa.public-key}") String publicKeyEncoded) throws NoSuchAlgorithmException, InvalidKeySpecException {
        this.publicKeyEncoded = publicKeyEncoded;
        try {
            this.jwkSet = buildJwkSet(publicKeyEncoded);
        } catch (ParseException e) {
            throw new IllegalStateException("Failed to parse JWK Set", e);
        }
    }

    private JWKSet buildJwkSet(String publicKeyEncoded) throws NoSuchAlgorithmException, InvalidKeySpecException, ParseException {
        byte[] keyBytes = Base64.getDecoder().decode(publicKeyEncoded);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        PublicKey publicKey = kf.generatePublic(spec);

        if (!(publicKey instanceof RSAPublicKey)) {
            throw new IllegalArgumentException("Provided public key is not an RSA public key.");
        }

        String keyId = UUID.randomUUID().toString();

        RSAKey rsaJwk = new RSAKey.Builder((RSAPublicKey) publicKey)
                .keyID(keyId)
                .build();

        return new JWKSet(rsaJwk);
    }


    @GetMapping("/.well-known/jwks.json")
    public Map<String, Object> jwks() {
        return jwkSet.toJSONObject();
    }
}