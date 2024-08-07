package com.qimiao.social.task;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Objects;

@Embeddable
public class ClientPrincipalId {
    @Column(name = "client_registration_id")
    private String clientRegistrationId;

    @Column(name = "principal_name")
    private String principalName;

    public ClientPrincipalId(String clientRegistrationId, String principalName) {
        this.clientRegistrationId = clientRegistrationId;
        this.principalName = principalName;
    }

    public ClientPrincipalId() {
    }

    public String getClientRegistrationId() {
        return clientRegistrationId;
    }

    public void setClientRegistrationId(String clientRegistrationId) {
        this.clientRegistrationId = clientRegistrationId;
    }

    public String getPrincipalName() {
        return principalName;
    }

    public void setPrincipalName(String principalName) {
        this.principalName = principalName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ClientPrincipalId that = (ClientPrincipalId) o;
        return Objects.equals(getClientRegistrationId(), that.getClientRegistrationId()) && Objects.equals(getPrincipalName(), that.getPrincipalName());
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(getClientRegistrationId());
        result = 31 * result + Objects.hashCode(getPrincipalName());
        return result;
    }
}
