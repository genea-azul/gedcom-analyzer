package com.geneaazul.gedcomanalyzer.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.PrintWriter;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminTokenFilterTest {

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain filterChain;
    @Mock
    private PrintWriter writer;

    private GedcomAnalyzerProperties properties;
    private AdminTokenFilter filter;

    @BeforeEach
    void setUp() {
        properties = new GedcomAnalyzerProperties();
        filter = new AdminTokenFilter(properties);
    }

    @Test
    void noTokenConfigured_protectedPath_allowsRequest() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/admin/comments/latest");

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(response, never()).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    @Test
    void tokenConfigured_protectedPath_missingToken_blocksRequest() throws Exception {
        properties.setAdminToken("secret");
        when(request.getRequestURI()).thenReturn("/api/admin/comments/latest");
        when(response.getWriter()).thenReturn(writer);

        filter.doFilter(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void tokenConfigured_protectedPath_wrongToken_blocksRequest() throws Exception {
        properties.setAdminToken("secret");
        when(request.getRequestURI()).thenReturn("/api/admin/comments/latest");
        when(request.getParameter("token")).thenReturn("wrong");
        when(response.getWriter()).thenReturn(writer);

        filter.doFilter(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void tokenConfigured_protectedPath_correctToken_allowsRequest() throws Exception {
        properties.setAdminToken("secret");
        when(request.getRequestURI()).thenReturn("/api/admin/comments/latest");
        when(request.getParameter("token")).thenReturn("secret");

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(response, never()).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    @Test
    void tokenConfigured_unprotectedPath_allowsRequestWithoutToken() throws Exception {
        properties.setAdminToken("secret");
        when(request.getRequestURI()).thenReturn("/api/comments");

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(response, never()).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    @Test
    void tokenConfigured_commentsPublicPath_allowsRequestWithoutToken() throws Exception {
        properties.setAdminToken("secret");
        when(request.getRequestURI()).thenReturn("/api/comments");

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void tokenConfigured_treeBuilderPath_correctToken_allowsRequest() throws Exception {
        properties.setAdminToken("secret");
        when(request.getRequestURI()).thenReturn("/tree-builder/latest");
        when(request.getParameter("token")).thenReturn("secret");

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

}
