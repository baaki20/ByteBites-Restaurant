package com.bytebites.authservice.service;

import com.bytebites.authservice.dto.AuthRequest;
import com.bytebites.authservice.dto.AuthResponse;
import com.bytebites.authservice.enums.RoleName;
import com.bytebites.authservice.model.Role;
import com.bytebites.authservice.model.User;
import com.bytebites.authservice.repository.RoleRepository;
import com.bytebites.authservice.repository.UserRepository;
import com.bytebites.authservice.util.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
@RequiredArgsConstructor
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public String registerUser(String email, String password) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("User with this email already exists.");
        }

        Set<Role> defaultRoles = new HashSet<>();
        Role customerRole = roleRepository.findByName(RoleName.ROLE_CUSTOMER)
                .orElseThrow(() -> new UsernameNotFoundException("Role not found."));

        defaultRoles.add(customerRole);

        User newUser = User.builder()
                .email(email)
                .password(passwordEncoder.encode(password))
                .roles(defaultRoles)
                .build();

        userRepository.save(newUser);
        return "User registered successfully.";
    }

    public AuthResponse authenticateAndGenerateToken(String email, String password) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, password)
            );

            User user = (User) authentication.getPrincipal();
            Set<String> roleNames = new HashSet<>();
            user.getRoles().forEach(role -> roleNames.add(role.getName().name()));

            String primaryRole = roleNames.isEmpty() ? null : roleNames.iterator().next();

            String accessToken = jwtService.generateToken(user.getId(), user.getEmail(), roleNames);

            return new AuthResponse(
                    accessToken,
                    "Bearer",
                    null,
                    jwtService.getExpiration(),
                    primaryRole
            );
        }catch(BadCredentialsException e){
            throw new BadCredentialsException("Invalid Email or Password");
        }
    }
}