package com.qimiao.social.calendars;

import com.microsoft.kiota.RequestInformation;
import com.microsoft.kiota.authentication.AuthenticationProvider;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Map;

public  class SimpleAuthProvider implements AuthenticationProvider {
        private final String accessToken;
        public SimpleAuthProvider(String accessToken) {
            this.accessToken = accessToken;
        }

    @Override
    public void authenticateRequest(@Nonnull RequestInformation request, @Nullable Map<String, Object> additionalAuthenticationContext) {
        request.headers.add("Authorization", "Bearer " + accessToken);
    }
    }