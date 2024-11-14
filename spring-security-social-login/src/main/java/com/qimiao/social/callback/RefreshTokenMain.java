package com.qimiao.social.callback;

import com.microsoft.aad.msal4j.ClientCredentialFactory;
import com.microsoft.aad.msal4j.ConfidentialClientApplication;
import com.microsoft.aad.msal4j.IAuthenticationResult;
import com.microsoft.aad.msal4j.RefreshTokenParameters;

import java.net.MalformedURLException;
import java.time.Instant;
import java.util.HashSet;

public class RefreshTokenMain {

    public static final String APPLICATION_NAME = "NBExampleApplication";
    public static final String TENANT_ID = "cf7cc825-b528-4694-879e-c25c0cd91ac7";
    public static final String CLIENT_ID = "a1a42d95-2a30-495e-ab6e-311e9611b801";
    public static final String CLIENT_SECRET = "4bs8Q~CRCzVd_qACFaLOv5tFUWhpaUHDSpAKJahj";
    public static final String CALL_BACK_URL = "https://bd0e-113-104-201-91.ngrok-free.app/notifications/outlook";

    public static void main(String[] args) {

        HashSet<String> scopesSet = new HashSet<>();
        scopesSet.add("offline_access");
        scopesSet.add("openid");
        scopesSet.add("email");
        scopesSet.add("Calendars.Read");
        scopesSet.add("Channel.Create");
        scopesSet.add("Channel.Delete.All");
        scopesSet.add("Channel.ReadBasic.All");

        String refreshToken = "M.C520_SN1.0.U.-CgHAM6IVMwi9GGfaJW!Kgl5PVkL3UbXtvbLQcZHgFQeTMvZdWEcInhK7syzh*5ertTi14*O6zBZsnaACIVGg5Az3Ni2ERdxl4mNJWw1uRaKgOOWF4EnFrk4QvoQqAo2ExislyJQJjf9yrPNYrIAUeajz*WJYJKnHiykrBa1cXxfPZEHBLnOahnO73dsMnzH6ERVkwVwzwr9Vhv9yxgXmXZprBmns6nZWUIi0*qHDKiiUc2wnC2DzWuhbEVm2nO8X51Q3hD*Qx5dWVvPA6b9VF*a4U!3jfcSoW0Wi31xUpwWWITlTBb60Aw*K38*Whest0KHGVC6WNxYbm8qf5TEooH0a7k44pE1bG021hSekncNu";
        String accessToken = "EwB4A8l6BAAUbDba3x2OMJElkF7gJ4z/VbCPEz0AAcu8bEvtUWRhtvxzZRTW0dWyCsa6Fqb9kFUZsJKxDDlXOTBKP/VJ3l3El4mOUOttYf4K0ScQyn/NkxWhXjcM5fbYzM5m9M/FSX+A/n8da77KygGdcbgn9kO2sOB2q0oQ0SSBBbjNJq5/EUqyP974srYLLvVVKw4SouqIObBJiVaTRWAiL7P2hQWhkhXRJSoG8I1EaTbrZb/Hp0Qe4oL9HPNexGmjVacdQFJGPwWERCyFhTt3vqGSH3dj9T4tb/+OS53UiMYEDsgf8LYm4Qv3+RaLd+cMrnlyh2912UhJUBJGMKtVqTvCGr3Hb+3TBg+flMPWt9AQC+bdhFPSrZpeeUkQZgAAEKFQ3dx3ApSQZ77S+YOHT0pAArV9ib/pCSbH562wor6MWrr+w+WVx2xT6pZfbSMM19g612URtpa3cCEvpL/WIxf3btn0GCyfvmVz5SjlDnNV0Scpisla47EMzS1qpKgXU7FoasB9/Rd6mrguoGuZ0QVJuH3tg4A2Uq7nDxv4F4AuSrwHIt6Yqnt6I2Wpze44IVbAdFRHa7tbrRK6ZtAHtmXg5M4IKoc7Fo91/7X/VmXDZQy6ukTKyBdcWz6x7awvkAG8HkJX74tsRl3rqQXdaCPfWvGnMHq/bky58Fncu4FMTGSC232X8QvoQvGCbedsXvMGzzpTBhxvqL4F4Neh28rt2QNuHlScZAg9MXAQt/NCUvoANjJemDjyB1qWTVO5hKHTX8ZaOWCAfamotQIqtQuDgIdXW6kjnebp7tHaSbbovII05I3gYF/8fadjA5eBY5QBR9oiKfzJlo6FHubqGW4i75l+5rsmCPrP7lk4NZBdtS4WirLOOYWBit8D7nf1PkGIXUOg9LR+Oa0jEixGWJ2VXMYelDpWKSRCWvCGGyBhTjXocN6v3hlIgHNBrWr14OJci/W6qD6yha7priz00+Uo9GAA+Hd+ValU+RLVNDaY6VfoRlFFMqRtPC2TMnAJO64kCM63AAgEBTAosp+aJh8uq1rDjAFZP/vefLW0eXWlfF+UknpBJ2znG1YrtkZfTYE/xJcioxRcOvEaK9ZPU1PJGxJA41z0NUI5BgZNeXUGOfjnAv74weMJYCtGx4/4+Af1yQVIidCLxOZkw77vpver7YoC";


        try {
            ConfidentialClientApplication app = ConfidentialClientApplication.builder(CLIENT_ID, ClientCredentialFactory.createFromSecret(CLIENT_SECRET))
                    .authority("https://login.microsoftonline.com/common/oauth2/v2.0/authorize")
                    .build();

            RefreshTokenParameters parameters = RefreshTokenParameters.builder(scopesSet, refreshToken)
                    .tenant(TENANT_ID)
                    .build();

            IAuthenticationResult authenticationResult = app.acquireToken(parameters).join();
            if (authenticationResult == null) {
                return;
            }

            String newAccessToken = authenticationResult.accessToken();
            Instant expiresDate = authenticationResult.expiresOnDate().toInstant();

            // 如何获得refreshToken？

            System.out.println("Old Refresh token: " + refreshToken);
            System.out.println("Old Access token: " + accessToken);
            System.out.println("New Access token: " + newAccessToken);

        } catch (MalformedURLException exception) {
            exception.printStackTrace();
        }
    }
}
