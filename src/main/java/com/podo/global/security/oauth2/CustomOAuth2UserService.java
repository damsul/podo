package com.podo.global.security.oauth2;

import com.podo.domain.user.entity.Role;
import com.podo.domain.user.entity.SocialType;
import com.podo.domain.user.entity.User;
import com.podo.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        SocialType socialType = SocialType.valueOf(registrationId.toUpperCase());

        Map<String, Object> attributes = oAuth2User.getAttributes();
        OAuth2UserInfo userInfo = OAuth2UserInfoFactory.of(socialType, attributes);

        User user = findOrCreateUser(userInfo);

        log.info("\uD83C\uDF47 소셜 로그인 성공: {} ({})", user.getName(), socialType);

        return new CustomOAuth2User(user, attributes);
    }

    private User findOrCreateUser(OAuth2UserInfo userInfo) {
        String loginId = userInfo.generateLoginId();

        return userRepository.findByLoginId(loginId)
                .orElseGet(() -> createNewUser(userInfo, loginId));
    }

    private User createNewUser(OAuth2UserInfo userInfo, String loginId) {
        User newUser = User.builder()
                .loginId(loginId)
                .name(userInfo.getName())
                .profileImageUrl(userInfo.getProfileImageUrl())
                .socialType(userInfo.getSocialType())
                .role(Role.USER)
                .build();

        return userRepository.save(newUser);
    }

}
