package com.qimiao.social.task;


import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomOAuth2AuthorizedClientRepository extends JpaRepository<OAuth2AuthorizedClientEntity, ClientPrincipal> {
}
