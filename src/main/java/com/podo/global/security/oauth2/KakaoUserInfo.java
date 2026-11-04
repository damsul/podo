package com.podo.global.security.oauth2;

import com.podo.domain.user.entity.SocialType;

import java.util.Map;

public class KakaoUserInfo extends OAuth2UserInfo{
    
    public KakaoUserInfo(Map<String, Object> attributes) {
        super(attributes);
    }

    @Override
    public SocialType getSocialType() {
        return SocialType.KAKAO;
    }
    
    @Override
    public String getProviderId() {
        return String.valueOf(attributes.get("id"));
    }

    @Override
    public String getName() {
        return (String) getProfile().get("nickname");
    }

    @Override
    public String getProfileImageUrl() {
        return (String) getProfile().get("profile_image_url");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getProfile() {
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        return (Map<String, Object>) kakaoAccount.get("profile");
    }
}
