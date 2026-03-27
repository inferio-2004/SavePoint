package com.example.Savepoint.Auth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.client.RestTemplate;

@Configuration
public class SecurityConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults()) // remove this if u don't want the cors config bean in the TodoAppApplication.java file
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/auth/**","/test/igdb","/test/igdb/testing_platforms","/api/games/seed").permitAll()
                        .anyRequest().authenticated()
                ).sessionManagement(
                        session->session.sessionCreationPolicy(SessionCreationPolicy.ALWAYS)
                ).exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(401);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"status\":401,\"message\":\"Invalid email or password\"}");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(403);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"status\":403,\"message\":\"Access denied\"}");
                        })
                );
                //.httpBasic(Customizer.withDefaults());

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(SteamAuthProvider steamAuthProvider, UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        var manualAuthenticationProvider = new DaoAuthenticationProvider(userDetailsService);
        manualAuthenticationProvider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(steamAuthProvider,manualAuthenticationProvider);
    }
}
