package com.geneaazul.gedcomanalyzer.config;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

import jakarta.annotation.Nullable;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;

/**
 * Gates admin pages/endpoints behind a static token (env var ADMIN_TOKEN), passed as
 * ?token=... so the plain admin pages stay bookmarkable, or as an X-Admin-Token header.
 * If ADMIN_TOKEN is unset the admin routes are open locally, but closed in production:
 * a missing secret must not expose the admin endpoints.
 */
@Component
@RequiredArgsConstructor
public class AdminTokenFilter extends OncePerRequestFilter {

    private static final List<String> PROTECTED_PATH_PREFIXES = List.of(
            "/api/admin/",
            "/search-family/",
            "/search-connection/",
            "/tree-builder/",
            "/comments/latest");

    static final String TOKEN_HEADER = "X-Admin-Token";

    private final GedcomAnalyzerProperties properties;
    private final Environment environment;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String adminToken = properties.getAdminToken();
        String requestPath = request.getRequestURI();

        boolean isProtectedPath = PROTECTED_PATH_PREFIXES.stream().anyMatch(requestPath::startsWith);
        boolean isAuthorized = StringUtils.isBlank(adminToken)
                ? !environment.matchesProfiles("prod", "flyio")
                : matches(adminToken, request.getHeader(TOKEN_HEADER)) || matches(adminToken, request.getParameter("token"));

        if (isProtectedPath && !isAuthorized) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write("Unauthorized");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private static boolean matches(String expected, @Nullable String actual) {
        return actual != null && MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8));
    }

}
