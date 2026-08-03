package com.geneaazul.gedcomanalyzer.config;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;

/**
 * Gates admin pages/endpoints behind a static token (env var ADMIN_TOKEN), passed as
 * ?token=... so the plain admin pages stay bookmarkable. If ADMIN_TOKEN is unset, this
 * filter is a no-op (dev/test convenience) — admin routes are open, same as before.
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

    private final GedcomAnalyzerProperties properties;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String adminToken = properties.getAdminToken();
        String requestPath = request.getRequestURI();

        boolean isProtectedPath = PROTECTED_PATH_PREFIXES.stream().anyMatch(requestPath::startsWith);
        boolean isAuthorized = StringUtils.isBlank(adminToken) || adminToken.equals(request.getParameter("token"));

        if (isProtectedPath && !isAuthorized) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write("Unauthorized");
            return;
        }

        filterChain.doFilter(request, response);
    }

}
