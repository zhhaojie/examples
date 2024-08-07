package com.qimiao.social.task;


import org.springframework.data.repository.ListCrudRepository;

public interface CustomOAuth2AuthorizedClientRepository extends ListCrudRepository<OAuth2AuthorizedClientEntity, ClientPrincipalId> {
}
