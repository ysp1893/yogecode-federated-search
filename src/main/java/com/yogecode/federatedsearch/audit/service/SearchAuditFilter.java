package com.yogecode.federatedsearch.audit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yogecode.federatedsearch.api.search.SearchRequest;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Component
public class SearchAuditFilter extends OncePerRequestFilter {

    private static final String SEARCH_PATH = "/api/search";
    private static final String DEFAULT_AUTHOR = "admin";
    private static final Long DEFAULT_CLIENT_ID = 1L;

    private final SearchAuditService searchAuditService;
    private final ObjectMapper objectMapper;

    public SearchAuditFilter(SearchAuditService searchAuditService, ObjectMapper objectMapper) {
        this.searchAuditService = searchAuditService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equalsIgnoreCase(request.getMethod()) || !request.getRequestURI().endsWith(SEARCH_PATH);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        Instant requestReceiveTime = Instant.now();
        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

        try {
            filterChain.doFilter(requestWrapper, responseWrapper);
        } finally {
            searchAuditService.record(new SearchAuditRecord(
                    readPayload(requestWrapper.getContentAsByteArray(), requestWrapper.getCharacterEncoding()),
                    readPayload(responseWrapper.getContentAsByteArray(), responseWrapper.getCharacterEncoding()),
                    requestReceiveTime,
                    Instant.now(),
                    resolveQuery(requestWrapper),
                    DEFAULT_AUTHOR,
                    DEFAULT_CLIENT_ID,
                    responseWrapper.getStatus() < 400 ? "SUCCESS" : "FAIL",
                    responseWrapper.getStatus()
            ));
            responseWrapper.copyBodyToResponse();
        }
    }

    private String resolveQuery(ContentCachingRequestWrapper requestWrapper) {
        String requestPayload = readPayload(requestWrapper.getContentAsByteArray(), requestWrapper.getCharacterEncoding());
        if (requestPayload == null || requestPayload.isBlank()) {
            return null;
        }
        try {
            SearchRequest searchRequest = objectMapper.readValue(requestPayload, SearchRequest.class);
            if (searchRequest.keyword() != null && !searchRequest.keyword().isBlank()) {
                return searchRequest.keyword();
            }
            return searchRequest.entity();
        } catch (Exception exception) {
            return null;
        }
    }

    private String readPayload(byte[] body, String characterEncoding) {
        if (body == null || body.length == 0) {
            return "";
        }
        Charset charset = characterEncoding == null
                ? StandardCharsets.UTF_8
                : Charset.forName(characterEncoding);
        return new String(body, charset);
    }
}
