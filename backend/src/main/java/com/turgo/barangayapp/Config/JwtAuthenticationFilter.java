package com.turgo.barangayapp.Config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        
        final String authorizationHeader = request.getHeader("Authorization");

        String email = null;
        String jwt = null;
        Boolean isAdmin = false;

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7);
            try {
                email = jwtUtil.extractEmail(jwt);
                isAdmin = jwtUtil.extractIsAdmin(jwt);
            } catch (Exception e) {
                // Invalid token, continue without authentication
            }
        }

        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            if (jwtUtil.validateToken(jwt, email)) {
                // Use Boolean.TRUE.equals to avoid NPE when isAdmin claim is null (treat as non-admin)
                boolean hasAdminRole = Boolean.TRUE.equals(isAdmin);
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        email, null, 
                        hasAdminRole ? Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN")) 
                                : Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }
        chain.doFilter(request, response);
    }
}
