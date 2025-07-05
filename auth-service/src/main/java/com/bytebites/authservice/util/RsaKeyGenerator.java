package com.bytebites.authservice.util;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;

public class RsaKeyGenerator {

    public static void main(String[] args) throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        PrivateKey privateKey = keyPair.getPrivate();
        PublicKey publicKey = keyPair.getPublic();

        String encodedPrivateKey = Base64.getEncoder().encodeToString(privateKey.getEncoded());
        String encodedPublicKey = Base64.getEncoder().encodeToString(publicKey.getEncoded());

        System.out.println("--- RSA Private Key (Base64 encoded) ---");
        System.out.println(encodedPrivateKey);
        System.out.println("\n--- RSA Public Key (Base64 encoded) ---");
        System.out.println(encodedPublicKey);

        System.out.println("\nAdd these to your application.yml/config files:");
        System.out.println("jwt.rsa.private-key: " + encodedPrivateKey);
        System.out.println("jwt.rsa.public-key: " + encodedPublicKey);
    }
}