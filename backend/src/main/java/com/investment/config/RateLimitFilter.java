package com.investment.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory rate limiter for auth endpoints.
 * Allows max 10 requests per minute per IP.
 */
@Component
public class RateLimitFilter implements Filter {

    private static final int MAX_REQUESTS = 10;
    private static final long WINDOW_MS = 60_000L;

    private final Map<String, long[]> buckets = new ConcurrentHashMap<>();

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        String path = request.getRequestURI();

        if (!path.startsWith("/api/auth/")) {
            chain.doFilter(req, res);
            return;
        }

        String ip = request.getRemoteAddr();
        long now = System.currentTimeMillis();
        long[] window = buckets.computeIfAbsent(ip, k -> new long[]{now, 0});

        synchronized (window) {
            if (now - window[0] > WINDOW_MS) {
                window[0] = now;
                window[1] = 0;
            }
            if (window[1] >= MAX_REQUESTS) {
                HttpServletResponse response = (HttpServletResponse) res;
                response.setStatus(429);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"请求过于频繁，请稍后再试\"}");
                return;
            }
            window[1]++;
        }

        chain.doFilter(req, res);
    }
}
