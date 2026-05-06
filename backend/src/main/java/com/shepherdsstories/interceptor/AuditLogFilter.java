package com.shepherdsstories.interceptor;

import com.shepherdsstories.config.UserAuthConfig;
import com.shepherdsstories.services.AuditLogService;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Component
public class AuditLogFilter implements Filter {

    private final AuditLogService auditLogService;

    public AuditLogFilter(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        chain.doFilter(request, response);

        if (request instanceof HttpServletRequest httpRequest && response instanceof HttpServletResponse httpResponse) {
            handleLogging(httpRequest, httpResponse);
        }
    }

    private void handleLogging(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        String uri = httpRequest.getRequestURI();
        String method = httpRequest.getMethod();

        if (uri.startsWith("/api/") && !uri.startsWith("/api/auth/")) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (isAuthenticatedUser(authentication)) {
                String email = authentication.getName();
                UUID userId = getUserId(authentication);
                String action = method + " " + uri;
                String details = "Status: " + httpResponse.getStatus();

                if (!method.equals("GET") || isNotableGetAction(uri)) {
                    auditLogService.log(action, email, userId, details, httpRequest.getRemoteAddr());
                }
            }
        }
    }

    private boolean isAuthenticatedUser(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated() && !isAnonymous(authentication);
    }

    private UUID getUserId(Authentication authentication) {
        if (authentication.getPrincipal() instanceof UserAuthConfig.AppUserDetails details) {
            return details.getId();
        }
        return null;
    }

    private boolean isAnonymous(Authentication authentication) {
        return authentication instanceof org.springframework.security.authentication.AnonymousAuthenticationToken;
    }

    private boolean isNotableGetAction(String uri) {
        return uri.contains("/feed") || uri.endsWith("/profile") || uri.contains("/missionary/") || uri.contains("/supporter/");
    }
}
