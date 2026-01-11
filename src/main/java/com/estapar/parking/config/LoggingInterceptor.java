package com.estapar.parking.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

@Component
public class LoggingInterceptor implements HandlerInterceptor {
    
    private static final Logger logger = LoggerFactory.getLogger(LoggingInterceptor.class);
    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String CORRELATION_ID_MDC_KEY = "correlationId";
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String correlationId = getCorrelationId(request);
        MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
        response.setHeader(CORRELATION_ID_HEADER, correlationId);
        
        logger.debug("Incoming request: method={}, uri={}, correlationId={}", 
                    request.getMethod(), request.getRequestURI(), correlationId);
        
        return true;
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, 
                               Object handler, @Nullable Exception ex) {
        String correlationId = MDC.get(CORRELATION_ID_MDC_KEY);
        int statusCode = response.getStatus();
        
        if (ex != null) {
            logger.error("Request completed with error: method={}, uri={}, status={}, correlationId={}, error={}", 
                        request.getMethod(), request.getRequestURI(), statusCode, correlationId, ex.getMessage());
        } else {
            logger.debug("Request completed: method={}, uri={}, status={}, correlationId={}", 
                        request.getMethod(), request.getRequestURI(), statusCode, correlationId);
        }
        
        MDC.clear();
    }
    
    private String getCorrelationId(HttpServletRequest request) {
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        return correlationId;
    }
}
