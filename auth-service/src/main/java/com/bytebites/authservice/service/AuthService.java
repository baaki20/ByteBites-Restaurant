package com.bytebites.authservice.service;

import com.bytebites.authservice.dto.AuthRequest;
import com.bytebites.authservice.model.User;
import com.bytebites.authservice.repository.UserRepository;
import com.bytebites.authservice.util.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService, AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    public String registerUser(String email, String password) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("User with this email already exists.");
        }

        Set<String> defaultRoles = new HashSet<>(Collections.singletonList("ROLE_CUSTOMER"));

        User newUser = User.builder()
                .email(email)
                .password(passwordEncoder.encode(password))
                .roles(defaultRoles)
                .build();

        userRepository.save(newUser);
        return "User registered successfully.";
    }

    public String authenticateAndGenerateToken(String email, String password) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, password)
        );

        if (authentication.isAuthenticated()) {
            UserDetails authenticatedUser = (UserDetails) authentication.getPrincipal();
            return jwtService.generateToken(authenticatedUser);
        } else {
            throw new UsernameNotFoundException("Invalid user credentials!");
        }
    }
}