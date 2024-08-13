package com.qimiao.social.task;

import com.microsoft.aad.msal4j.ClientCredentialFactory;
import com.microsoft.aad.msal4j.ConfidentialClientApplication;
import com.microsoft.aad.msal4j.IAuthenticationResult;
import com.microsoft.aad.msal4j.RefreshTokenParameters;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class X {

    private static final String CLIENT_ID = "a1a42d95-2a30-495e-ab6e-311e9611b801";
    private static final String CLIENT_SECRET = "4bs8Q~CRCzVd_qACFaLOv5tFUWhpaUHDSpAKJahj";
    private static final String TENANT_ID = "cf7cc825-b528-4694-879e-c25c0cd91ac7";
    private static final String AUTHORITY = "https://login.microsoftonline.com/common/oauth2/authorize";

    public static void main(String[] args) throws Exception {
        // 已经保存的 refresh_token
        String refreshToken = "M.C520_BAY.0.U.-Cql0l5zIxzMW0MgVvn4OxdwmSJHF4UdNY2llasUU!Lr0K7L6rUZkI13B1G74QUT0YFLWD7GH53qVSPhbuW5PXQtBR2U9iTbNyWxPLGotzbC9z9H8yZc9IpJxQuRA*Cz34ZtTF2lifCiGDDXflHsncLQisJWOQhuCkgFKY7oJpJkhSTBYxAly*G1JPV6SsIXaennnrHc1NjqnhX**VbwjdZ3gH7esJ9bCZuLriszjeFkK75fZEIhw*TLylWmYhsbRlMj3FIxLBtuhK*swpkZBCjFasGrnbnem!zx1O3Mzo9xxXfNxvndqmPPyDQVimXrwJOG4o2671m1aGrHwnEJ0oNOBVpEzC2rOjhdOhzTItCxU";

        // 构建 ClientCredentialParameters
        final String[] ss = new String[]{
                "offline_access",
                "openid",
                "email",
                "profile",
                "Calendars.Read",
                "Channel.Create",
                "Channel.Delete.All",
                "Channel.ReadBasic.All"
        };
        Set<String> scopes = new HashSet<>(Arrays.asList(ss));

        ConfidentialClientApplication app = ConfidentialClientApplication.builder(CLIENT_ID, ClientCredentialFactory.createFromSecret(CLIENT_SECRET))
                .authority(AUTHORITY)
                .build();

        RefreshTokenParameters parameters = RefreshTokenParameters.builder(scopes, refreshToken)
                .tenant(TENANT_ID)
                .build();

        IAuthenticationResult result = app.acquireToken(parameters).join();
        String newAccessToken = result.accessToken();

        System.out.println("New Access Token: " + newAccessToken);

    }
}

