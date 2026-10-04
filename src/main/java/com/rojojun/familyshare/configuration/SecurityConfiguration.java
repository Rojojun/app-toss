package com.rojojun.familyshare.configuration;

import com.rojojun.familyshare.auth.BearerTokenAuthenticationFilter;
import com.rojojun.familyshare.auth.HmacApiTokenVerifier;
import com.rojojun.familyshare.auth.JsonAuthenticationEntryPoint;
import com.rojojun.familyshare.auth.UserSessionValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.FormLoginConfigurer;
import org.springframework.security.config.annotation.web.configurers.HttpBasicConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfiguration {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            HmacApiTokenVerifier tokenVerifier,
            UserSessionValidator sessionValidator,
            JsonAuthenticationEntryPoint authenticationEntryPoint
    ) throws Exception {
        var bearerFilter = new BearerTokenAuthenticationFilter(tokenVerifier, sessionValidator);

       return http
               .csrf(AbstractHttpConfigurer::disable)
               .cors(Customizer.withDefaults())
               .formLogin(FormLoginConfigurer::disable)
               .httpBasic(HttpBasicConfigurer::disable)
               .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
               .exceptionHandling(exception -> exception
                       .authenticationEntryPoint(authenticationEntryPoint)
                       .accessDeniedHandler((request, response, accessDeniedException) -> response.setStatus(HttpStatus.FORBIDDEN.value()))
               )
               .authorizeHttpRequests(authorize -> authorize
                       .requestMatchers(HttpMethod.POST, "/auth/toss/exchange").permitAll()
                       .requestMatchers(HttpMethod.GET, "/health").permitAll()
                       .requestMatchers(HttpMethod.GET, "/invitations/*").permitAll()
                       .requestMatchers("/error").permitAll()
                       .anyRequest().authenticated()
               )
               .addFilterBefore(
                       bearerFilter,
                       UsernamePasswordAuthenticationFilter.class
               )
               .build();
    }
}
