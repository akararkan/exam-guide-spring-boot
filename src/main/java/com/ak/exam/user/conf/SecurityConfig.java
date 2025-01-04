package com.ak.exam.user.conf;

import com.ak.exam.user.exceptions.JwtAccessDeniedHandler;
import com.ak.exam.user.exceptions.JwtAuthenticationEntryPoint;
import com.ak.exam.user.jwt.JWTAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import static com.ak.exam.user.consts.SecurityConstants.*;
import static com.ak.exam.user.enums.Role.*;
import static org.springframework.http.HttpMethod.GET;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfig {

    public final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    public final JwtAccessDeniedHandler jwtAccessDeniedHandler;
    private final AuthenticationProvider authenticationProvider;
    public final JWTAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorize->{
                    authorize.requestMatchers(PUBLIC_URL).permitAll()
                            .requestMatchers("/error").permitAll()
                            .requestMatchers(GET, "/test/hi/**").hasAnyRole(ADMIN.name(),MANAGER.name())                            .anyRequest().authenticated();
                })
                .sessionManagement(sessionManagement -> sessionManagement
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS) // Maintain statelessness
                )
                .authenticationProvider(authenticationProvider) // Configure your authentication provider
                .exceptionHandling(ex->{
                    ex.accessDeniedHandler(jwtAccessDeniedHandler);
                    ex.authenticationEntryPoint(jwtAuthenticationEntryPoint);
                })
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }









}
