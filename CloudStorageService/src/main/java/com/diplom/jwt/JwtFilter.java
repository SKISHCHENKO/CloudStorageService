package com.diplom.jwt;

import com.diplom.service.MyUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@AllArgsConstructor
public class JwtFilter extends OncePerRequestFilter {
    private final MyUserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestPath = request.getRequestURI();

        // Пропускаем без проверки токена
        if (requestPath.startsWith("/users")) {
            filterChain.doFilter(request, response);
            return;
        }


        String token;
        String authHeader = request.getHeader("auth-token");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        } else {
            token = authHeader;
        }

        if (token != null && jwtUtil.validateToken(token)) {
            String email = jwtUtil.getEmail(token);
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);

            if (userDetails != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UsernamePasswordAuthenticationToken authenticationToken =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                log.info("✅ Authentication for user: {}", userDetails.getUsername());
            }
        } else {
            if (token == null) {
                log.error("❌ Token is missing.");
            } else if (!jwtUtil.validateToken(token)) {
                log.error("❌ Token is invalid.");
            } else {
                System.out.println("Error!");
            }
        }
        filterChain.doFilter(request, response);
    }
}