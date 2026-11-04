package com.podo.global.security.oauth2;

import com.podo.domain.user.entity.SocialType;

import java.util.Map;

public class OAuth2UserInfoFactory {

    private OAuth2UserInfoFactory() {
    }

    public static OAuth2UserInfo of(SocialType socialType, Map<String, Object> attributes) {
        return switch (socialType) {
            case KAKAO -> new KakaoUserInfo(attributes);
            case GOOGLE -> new GoogleUserInfo(attributes);
            case NAVER -> new NaverUserInfo(attributes);
        };
    }
}
