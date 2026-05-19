package com.standup.testutil;

import com.standup.context.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import com.standup.interceptor.JwtInterceptor;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Component
@Primary
public class TestUserContextInterceptor extends JwtInterceptor implements HandlerInterceptor, WebMvcConfigurer {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer Test ")) {
            String[] parts = authHeader.split(" ");
            if (parts.length >= 4) {
                Long userId = Long.parseLong(parts[2]);
                String role = parts[3];
                UserContext.set(userId, role);
                return true;
            }
        }
        response.setStatus(401);
        return false;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor((HandlerInterceptor) this)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/login", "/api/auth/register");
    }
}
