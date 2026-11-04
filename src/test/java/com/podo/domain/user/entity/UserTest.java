package com.podo.domain.user.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    @Nested
    @DisplayName("User 생성 테스트")
    class CreateUser {

        @Test
        @DisplayName("일반 회원가입 사용자를 생성한다")
        void createNormalUser() {
            User user = User.builder()
                    .loginId("pien")
                    .password("encoded_password")
                    .name("최경환")
                    .email("pien@naver.com")
                    .build();

            assertThat(user.getLoginId()).isEqualTo("pien");
            assertThat(user.getName()).isEqualTo("최경환");
            assertThat(user.getRole()).isEqualTo(Role.USER);
            assertThat(user.isDeleted()).isFalse();
            assertThat(user.isSocialUser()).isFalse();
        }

        @Test
        @DisplayName("소셜 로그인 사용자를 생성한다")
        void createSocialUser() {
            User user = User.builder()
                    .loginId("orange")
                    .name("박주황")
                    .email("orange@gmail.com")
                    .socialType(SocialType.GOOGLE)
                    .build();

            assertThat(user.getSocialType()).isEqualTo(SocialType.GOOGLE);
            assertThat(user.isSocialUser()).isTrue();
            assertThat(user.getPassword()).isNull(); // 소셜 가입은 password 없음
        }

        @Test
        @DisplayName("Role을 명시적으로 ADMIN으로 지정할 수 있다")
        void createAdminUser() {
            User admin = User.builder()
                    .loginId("admin")
                    .name("관리자")
                    .role(Role.ADMIN)
                    .build();

            assertThat(admin.getRole()).isEqualTo(Role.ADMIN);
        }
    }

    @Nested
    @DisplayName("User 비즈니스 메서드 테스트")
    class BusinessMethods {

        @Test
        @DisplayName("프로필을 업데이트할 수 있다")
        void updateProfile() {
            User user = User.builder()
                    .name("최경환")
                    .build();

            user.updateProfile("최경환2", "https://image.com/new.jpg");

            assertThat(user.getName()).isEqualTo("최경환2");
            assertThat(user.getProfileImageUrl()).isEqualTo("https://image.com/new.jpg");
        }

        @Test
        @DisplayName("비밀번호를 변경할 수 있다")
        void changePassword() {
            User user = User.builder()
                    .password("old_password")
                    .build();

            user.changePassword("new_password");

            assertThat(user.getPassword()).isEqualTo("new_password");
        }

        @Test
        @DisplayName("회원 탈퇴 시 delete가 true로 변경된다")
        void softDelete() {
            User user = User.builder()
                    .name("최경환")
                    .build();
            assertThat(user.isDeleted()).isFalse();

            user.softDelete();

            assertThat(user.isDeleted()).isTrue();
        }
    }
}