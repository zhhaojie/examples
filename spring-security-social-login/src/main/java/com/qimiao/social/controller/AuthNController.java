package com.qimiao.social.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
public class AuthNController {

    @GetMapping("/secured")
    public String secured() {
        return "Hello, Secured!";
    }

    @GetMapping("/noSecured")
    public String noSecured() {
        return "Hello, noSecured!";
    }

    @GetMapping("/")
    public String index(Principal principal) {
        return "Hello," + principal.getName();
    }

}
