package com.rojojun.familyshare.auth;

import com.rojojun.familyshare.common.CustomPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class BearerTokenAuthenticationFilter extends OncePerRequestFilter {

    private final HmacApiTokenVerifier tokenVerifier;
    private final UserSessionValidator sessionValidator;

    public BearerTokenAuthenticationFilter(HmacApiTokenVerifier tokenVerifier, UserSessionValidator sessionValidator) {
        this.tokenVerifier = tokenVerifier;
        this.sessionValidator = sessionValidator;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        extractBearerToken(request)
                .flatMap(this::verify)
                .filter(sessionValidator::isValid)
                .map(it -> new UsernamePasswordAuthenticationToken(new CustomPrincipal(it), null, List.of()))
                .ifPresent(SecurityContextHolder.getContext()::setAuthentication);

        filterChain.doFilter(request, response);
    }

    private Optional<String> extractBearerToken(HttpServletRequest request) {
        return Optional.ofNullable(request.getHeader("Authorization"))
                .filter(header -> header.regionMatches(true, 0, "Bearer ", 0, 7))
                .map(header -> header.substring(7).trim());
    }

    private Optional<UUID> verify(String token) {
        try {
            return Optional.of(tokenVerifier.verifyAndGetUserId(token));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
