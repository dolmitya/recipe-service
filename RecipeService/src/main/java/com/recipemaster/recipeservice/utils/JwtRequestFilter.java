package com.recipemaster.recipeservice.utils;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@AllArgsConstructor
@Slf4j
@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private final JwtTokenUtils jwtTokenUtils;

    @Override
    protected void doFilterInternal(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        String username = null;
        String jwtToken;

        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            jwtToken = authHeader.substring(BEARER_PREFIX.length());
            try {
                if (!jwtToken.isEmpty()) {
                    username = jwtTokenUtils.getUsername(jwtToken);
                } else {
                    log.warn("JWT token is empty or invalid.");
                }
            } catch (ExpiredJwtException e) {
                log.info("JWT token has expired: {}", jwtToken);
            } catch (MalformedJwtException e) {
                log.info("JWT token is malformed: {}", jwtToken);
            } catch (Exception e) {
                log.error("An error occurred while processing JWT token", e);
            }
        } else {
            log.warn("Authorization header is missing or does not start with 'Bearer '.");
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(username,
                    null, Collections.emptyList());

            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }
}