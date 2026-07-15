package com.hollysys.ppa.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * SPA 路由回退 — 所有非 /api/** 请求回退到 index.html，由前端 Vue Router 处理
 */
@Component
public class SpaFallbackHandler implements HandlerExceptionResolver {

    @Override
    public org.springframework.web.servlet.ModelAndView resolveException(
            HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 只处理 404 且路径不是 /api/ 开头的情况
        if (response.getStatus() == 404 && !request.getRequestURI().startsWith("/api/")
                && !request.getRequestURI().startsWith("/swagger")
                && !request.getRequestURI().startsWith("/v3/api-docs")
                && !request.getRequestURI().startsWith("/actuator")) {
            try {
                request.getRequestDispatcher("/index.html").forward(request, response);
            } catch (Exception ignored) {
            }
            return new org.springframework.web.servlet.ModelAndView();
        }
        return null;
    }
}
