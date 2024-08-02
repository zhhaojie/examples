package com.qimiao.social.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.minidev.json.JSONObject;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;

import java.io.IOException;

public class CustomSavedRequestAwareAuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws ServletException, IOException {
        if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
            OAuth2User oauthUser = oauthToken.getPrincipal();

            // 获取用户的公开信息
            String username = oauthUser.getAttribute("login"); // GitHub 用户名
            String email = oauthUser.getAttribute("email"); // GitHub 邮箱地址

            // 在本地数据库中查找或创建用户

            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            JSONObject object = new JSONObject();
            object.put("code", 0);
            object.put("message", "ok");
            object.put("username", username);
            object.put("email", email);

            response.getWriter().write(object.toJSONString());
            response.getWriter().flush();
        }
    }
}