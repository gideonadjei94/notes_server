package com.gideon.notes.utils;

import com.gideon.notes.service.RateLimitService;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimitService rateLimitService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String key = getClientIdentifier(request);
        RateLimitService.RateLimitType rateLimitType = determineRateLimitType(request.getRequestURI(), request.getMethod());

        Bucket bucket = rateLimitService.resolveBucket(key, rateLimitType);
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            response.addHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
            return true;
        }

        long waitForRefill = probe.getNanosToWaitForRefill() / 1_000_000_000;
        response.addHeader("X-Rate-Limit-Retry-After-Seconds", String.valueOf(waitForRefill));
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json");
        response.getWriter().write(String.format(
                "{\"message\":\"Rate limit exceeded. Try again in %d seconds\",\"data\":null}",
                waitForRefill
        ));

        log.warn("Rate limit exceeded for client: {} on endpoint: {}", key, request.getRequestURI());
        return false;
    }

    private String getClientIdentifier(HttpServletRequest request) {
        String userPrincipal = request.getUserPrincipal() != null ? request.getUserPrincipal().getName() : null;

        if (userPrincipal != null && !userPrincipal.isEmpty()) {
            return userPrincipal;
        }

        String clientIp = request.getHeader("X-Forwarded-For");
        if (clientIp == null || clientIp.isEmpty()) {
            clientIp = request.getRemoteAddr();
        }
        return clientIp;
    }

    private RateLimitService.RateLimitType determineRateLimitType(String uri, String method) {
        if (uri.startsWith("/api/auth/")) {
            return RateLimitService.RateLimitType.AUTH;
        }

        if (uri.startsWith("/api/notes")) {
            if ("POST".equals(method) && !uri.contains("/restore")) {
                return RateLimitService.RateLimitType.NOTES_CREATE;
            }
            if ("PUT".equals(method)) {
                return RateLimitService.RateLimitType.NOTES_UPDATE;
            }
        }

        return RateLimitService.RateLimitType.API;
    }
}
