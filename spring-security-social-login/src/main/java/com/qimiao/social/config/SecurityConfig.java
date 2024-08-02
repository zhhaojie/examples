package com.qimiao.social.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers("/", "/error").permitAll();
                    auth.requestMatchers("/favicon.ico", "/webjars/**").permitAll();
                    auth.anyRequest().authenticated();
                })
                // 自有账号体系
                .formLogin(form -> form.defaultSuccessUrl("/"))
                // 第三方社交账号
                .oauth2Login(oauth2 -> oauth2.successHandler(new CustomSavedRequestAwareAuthenticationSuccessHandler()))
                .build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails user1 = User.withDefaultPasswordEncoder()
                .username("user1")
                .password("password1")
                .roles("USER")
                .build();

        UserDetails user2 = User.withDefaultPasswordEncoder()
                .username("user2")
                .password("password2")
                .roles("USER")
                .build();

        return new InMemoryUserDetailsManager(user1, user2);
    }

}
