package com.smartats.backend.auth;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        System.out.println(">>> JWT FILTER: path=" + request.getRequestURI() + " authHeader=" + authHeader);

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            boolean valid = jwtService.isValid(token);

            if (valid) {
                String email = jwtService.extractEmail(token);
                String role = jwtService.extractRole(token);
                System.out.println(">>> JWT FILTER: email=" + email + " role=" + role);

                var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
                var authentication = new UsernamePasswordAuthenticationToken(email, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
                System.out.println(">>> JWT FILTER: authentication posée dans le contexte");
            }
        } else {
            System.out.println(">>> JWT FILTER: pas de header Bearer");
        }

        filterChain.doFilter(request, response);
    }
}