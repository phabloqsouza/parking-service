package com.estapar.parking.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(1)
public class CorrelationIdFilter implements Filter {
    
    private static final Logger logger = LoggerFactory.getLogger(CorrelationIdFilter.class);
    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String CORRELATION_ID_MDC_KEY = "correlationId";
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        String correlationId = getCorrelationId(httpRequest);
        MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
        httpResponse.setHeader(CORRELATION_ID_HEADER, correlationId);
        
        String requestUri = httpRequest.getRequestURI();
        String method = httpRequest.getMethod();
        String clientIp = getClientIpAddress(httpRequest);
        
        if (requestUri.startsWith("/webhook")) {
            logger.info("Incoming webhook request: method={}, uri={}, clientIp={}, remoteAddr={}, userAgent={}, contentType={}", 
                       method, requestUri, clientIp, httpRequest.getRemoteAddr(), 
                       httpRequest.getHeader("User-Agent"), httpRequest.getContentType());
        }
        
        try {
            chain.doFilter(request, response);
            if (requestUri.startsWith("/webhook")) {
                logger.info("Webhook request completed: method={}, uri={}, status={}", 
                           method, requestUri, httpResponse.getStatus());
            }
        } catch (Exception e) {
            logger.error("Webhook request failed: method={}, uri={}, error={}", 
                        method, requestUri, e.getMessage(), e);
            throw e;
        } finally {
            MDC.clear();
        }
    }
    
    private String getCorrelationId(HttpServletRequest request) {
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        return correlationId;
    }
    
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }
}
