package com.ak.exam.user.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

import static com.ak.exam.user.consts.SecurityConstants.*;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@Component
@RequiredArgsConstructor
public class JWTAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {

        // Allow OPTIONS requests to proceed (for CORS preflight)
        if (request.getMethod().equalsIgnoreCase(OPTIONS_HTTP_METHOD)) {
            response.setStatus(HttpStatus.OK.value());
            return;
        }

        // Get the Authorization header
        String authorizationHeader = request.getHeader(AUTHORIZATION);

        // If Authorization header is missing or doesn't start with the expected prefix, continue the filter chain
        if (authorizationHeader == null || !authorizationHeader.startsWith(TOKEN_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Extract token by removing the "Bearer " prefix
        String token = authorizationHeader.substring(TOKEN_PREFIX.length());
        String username = jwtTokenProvider.getSubject(token);

        try {
            // Validate token and ensure there's no existing authentication in the context
            if (jwtTokenProvider.isTokenValid(username, token) && SecurityContextHolder.getContext().getAuthentication() == null) {

                // Get authorities from the token
                List<GrantedAuthority> authorities = jwtTokenProvider.getAuthorities(token);

                // Log extracted authorities for debugging
                System.out.println("Extracted Authorities: " + authorities);

                // Create an Authentication object and set it in the SecurityContext
                Authentication authentication = jwtTokenProvider.getAuthentication(username, authorities, request);
                SecurityContextHolder.getContext().setAuthentication(authentication);

            } else {
                SecurityContextHolder.clearContext(); // Clear the context if the token is invalid
            }
        } catch (Exception e) {
            e.printStackTrace(); // Log the error for debugging
            SecurityContextHolder.clearContext(); // Clear the context in case of any errors
            response.setStatus(HttpStatus.FORBIDDEN.value()); // Respond with 403 Forbidden if there's an issue
            response.getWriter().write("Access Denied: Invalid Token"); // You can customize this message
            return; // Stop further processing
        }

        // Continue the filter chain if no exception occurred
        filterChain.doFilter(request, response);
    }
}
