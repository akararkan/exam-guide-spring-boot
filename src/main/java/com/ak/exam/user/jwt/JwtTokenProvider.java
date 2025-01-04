package com.ak.exam.user.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.ak.exam.user.model.User;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static com.ak.exam.user.consts.SecurityConstants.*;

@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String SECRET;

    @Value("${jwt.expiration}")
    public long EXPIRATION_TIME;

    // Generates the JWT token with authorities as a claim
    public String generateToken(User user) {
        String[] claims = getClaimsFromUser(user);  // Get the authorities (roles/permissions)
        return JWT.create()
                .withIssuer(AKAR_ARKAN)
                .withAudience(AKAR_ARKAN_ADMINISTRATION)
                .withIssuedAt(new Date())
                .withSubject(user.getUsername()) // Subject is the username
                .withClaim(ID_CLAIM, user.getId()) // Store user ID
                .withClaim(ROLE, user.getRole().name()) // Store user role
                .withArrayClaim(AUTHORITIES, claims) // Store authorities as an array of strings
                .withExpiresAt(new Date(System.currentTimeMillis() + EXPIRATION_TIME)) // Set expiration
                .sign(Algorithm.HMAC256(SECRET.getBytes())); // Sign the token with the secret
    }

    // Converts the user's authorities into a string array for storage in the token
    private String[] getClaimsFromUser(User user) {
        return user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority) // Convert each authority to string
                .toArray(String[]::new); // Collect as string array
    }

    // Retrieve authentication object based on the token
    public Authentication getAuthentication(String username, List<GrantedAuthority> authorities, HttpServletRequest request) {
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(username, null, authorities);
        authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        return authenticationToken;
    }

    // Validate token expiration and existence of the subject (username)
    public boolean isTokenValid(String username, String token) {
        JWTVerifier verifier = getJWTVerifier();
        return StringUtils.isNotEmpty(username) && !isTokenExpired(verifier, token);
    }

    // Get the subject (username) from the token
    public String getSubject(String token) {
        try {
            JWTVerifier verifier = getJWTVerifier();
            return verifier.verify(token).getSubject(); // Extract the subject (username)
        } catch (JWTDecodeException e) {
            System.err.println("Token parsing error: " + e.getMessage());
            throw new IllegalArgumentException("Invalid token");
        }
    }

    // JWT Verifier
    private JWTVerifier getJWTVerifier() {
        try {
            Algorithm algorithm = Algorithm.HMAC256(SECRET); // Consistently use the same secret for verification
            return JWT.require(algorithm)
                    .withIssuer(AKAR_ARKAN) // Ensure this matches the issuer used when creating the JWT
                    .build();
        } catch (JWTVerificationException exception) {
            throw new RuntimeException("JWT verification failed: " + exception.getMessage());
        }
    }

    // Check if the token is expired
    private boolean isTokenExpired(JWTVerifier verifier, String token) {
        Date expiration = verifier.verify(token).getExpiresAt();
        return expiration.before(new Date());
    }

    // Get the authorities (roles/permissions) from the token
    public List<GrantedAuthority> getAuthorities(String token) {
        String[] claims = getAuthFromToken(token);
        return Arrays.stream(claims)
                .map(claim -> new SimpleGrantedAuthority(claim.replace(":", "_").toUpperCase())) // Ensure correct naming convention
                .collect(Collectors.toList());
    }

    // Extract the "authorities" claim from the token
    public String[] getAuthFromToken(String token) {
        JWTVerifier verifier = getJWTVerifier();
        return verifier.verify(token).getClaim("authorities").asArray(String.class); // Extract "authorities" as an array of strings
    }

    // Extract the user ID from the token
    public Long getUserIdFromToken(String token) {
        DecodedJWT decodedJWT = decodeToken(token); // Decode the token
        return decodedJWT.getClaim(ID_CLAIM).asLong(); // Return the user ID
    }

    // Decode the token
    private DecodedJWT decodeToken(String token) {
        JWTVerifier verifier = JWT.require(Algorithm.HMAC256(SECRET))
                .withIssuer(AKAR_ARKAN)
                .build();
        return verifier.verify(token);
    }
}
