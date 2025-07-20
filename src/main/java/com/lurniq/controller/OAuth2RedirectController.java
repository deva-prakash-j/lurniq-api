package com.lurniq.controller;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@Hidden
public class OAuth2RedirectController {

    @GetMapping("/oauth2/google")
    public void googleOAuth2Redirect(
            @RequestParam(required = false) String frontend,
            HttpServletResponse response
    ) throws IOException {
        
        if ("true".equals(frontend)) {
            // Store the frontend flag using a session-scoped approach
            // We'll use a different approach - encode it in the OAuth2 authorization URL
            System.out.println("Frontend OAuth2 request detected");
            
            // Redirect to OAuth2 with a special parameter that will be preserved
            response.sendRedirect("/oauth2/authorization/google?frontend=true");
        } else {
            // Regular OAuth2 flow
            System.out.println("Regular OAuth2 flow - no frontend flag");
            response.sendRedirect("/oauth2/authorization/google");
        }
    }
}
