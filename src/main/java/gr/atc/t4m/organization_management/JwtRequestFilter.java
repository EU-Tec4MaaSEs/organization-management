package gr.atc.t4m.organization_management;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.web.filter.OncePerRequestFilter;


import java.io.IOException;

public class JwtRequestFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String requestURI = request.getRequestURI();
        if ("/api/organization/health".equals(requestURI)) {
            chain.doFilter(request, response); // Skip JWT validation
            return;
        }

        // Your JWT validation logic here
        // If invalid, throw an AuthenticationException or handle it appropriately
        chain.doFilter(request, response);
    }
}

