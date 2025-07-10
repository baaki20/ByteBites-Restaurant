package com.bytebites.restaurantservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Spring Security filter to extract user information from custom headers
 * forwarded by the API Gateway and set it in the SecurityContext.
 */
public class GatewayHeaderAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String userEmail = request.getHeader("X-Auth-User");
        String userRoles = request.getHeader("X-Auth-Roles");

        if (userEmail != null && !userEmail.isEmpty() && userRoles != null && !userRoles.isEmpty()) {
            String[] roles = userRoles.split(",");
            String formattedRoles = Stream.of(roles)
                    .map(String::trim)
                    .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                    .collect(Collectors.joining(","));

            GatewayAuthenticationToken authentication = new GatewayAuthenticationToken(userEmail, formattedRoles);
            SecurityContextHolder.getContext().setAuthentication(authentication);

            logger.debug("Authentication set for user: " + userEmail + " with roles: " + formattedRoles);
        } else {
            logger.debug("X-Auth-User or X-Auth-Roles headers missing. Proceeding without authentication context.");
        }

        filterChain.doFilter(request, response);
    }
}