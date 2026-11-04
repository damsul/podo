package com.podo.global.security.oauth2;

import com.podo.domain.user.entity.SocialType;

import java.util.Map;

public class NaverUserInfo extends OAuth2UserInfo{

    public NaverUserInfo(Map<String, Object> attributes) {
        super(attributes);
    }

    @Override
    public SocialType getSocialType() {
        return SocialType.NAVER;
    }

    @Override
    public String getProviderId() {
        return (String) getResponse().get("id");
    }

    @Override
    public String getName() {
        return (String) getResponse().get("nickname");
    }

    @Override
    public String getProfileImageUrl() {
        return (String) getResponse().get("profile_image");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getResponse() {
        return (Map<String, Object>) attributes.get("response");
    }
}
