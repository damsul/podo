package com.podo.global.security.oauth2;

import com.podo.domain.user.entity.SocialType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Map;

@Getter
@RequiredArgsConstructor
public abstract class OAuth2UserInfo {

    protected final Map<String, Object> attributes;

    public abstract SocialType getSocialType();

    public abstract String getProviderId();

    public abstract String getName();

    public abstract String getProfileImageUrl();

    public String generateLoginId() {
        return getSocialType().name().toLowerCase() + "_" + getProviderId();
    }

}
