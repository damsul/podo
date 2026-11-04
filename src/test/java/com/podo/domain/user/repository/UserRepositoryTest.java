package com.podo.domain.user.repository;

import com.podo.domain.user.entity.Role;
import com.podo.domain.user.entity.SocialType;
import com.podo.domain.user.entity.User;
import com.podo.global.config.JpaAuditingConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaAuditingConfig.class)
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    private User savedUser;

    @BeforeEach
    void setUp() {
        User user = User.builder()
                .loginId("pien")
                .password("password123")
                .name("최경환")
                .email("pien@naver.com")
                .role(Role.USER)
                .build();

        savedUser = userRepository.save(user);
    }

    @Nested
    @DisplayName("기본 CRUD 테스트")
    class BasicCrud {

        @Test
        @DisplayName("사용자를 저장하면 ID가 생성된다")
        void savedUser() {
            assertThat(savedUser.getId()).isNotNull();
            assertThat(savedUser.getCreatedAt()).isNotNull();
            assertThat(savedUser.getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("ID로 사용자를 조회할 수 있다")
        void findById() {
            Optional<User> found = userRepository.findById(savedUser.getId());

            assertThat(found).isPresent();
            assertThat(found.get().getName()).isEqualTo("최경환");
        }
    }

    @Nested
    @DisplayName("커스텀 쿼리 메서드 테스트")
    class CustomQueries {

        @Test
        @DisplayName("loginId로 사용자를 조회할 수 있다")
        void findByLoginId() {
            Optional<User> found = userRepository.findByLoginId("pien");

            assertThat(found).isPresent();
            assertThat(found.get().getEmail()).isEqualTo("pien@naver.com");
        }

        @Test
        @DisplayName("존재하지 않는 loginId면 빈 Optional을 반환한다")
        void findByLoginId_notFound() {
            Optional<User> found = userRepository.findByLoginId("nonexistent");

            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("email로 사용자를 조회할 수 있다")
        void findByEmail() {
            Optional<User> found = userRepository.findByEmail("pien@naver.com");

            assertThat(found).isPresent();
            assertThat(found.get().getLoginId()).isEqualTo("pien");
        }

        @Test
        @DisplayName("loginId 존재 여부를 확인할 수 있다")
        void existsByLoginId() {
            assertThat(userRepository.existsByLoginId("pien")).isTrue();
            assertThat(userRepository.existsByLoginId("nonexistent")).isFalse();
        }

        @Test
        @DisplayName("email 존재 여부를 확인할 수 있다")
        void existsByEmail() {
            assertThat(userRepository.existsByEmail("pien@naver.com")).isTrue();
            assertThat(userRepository.existsByEmail("nobody@nowhere.com")).isFalse();
        }
    }

    @Nested
    @DisplayName("소셜 로그인 사용자 저장 테스트")
    class SocialUser {

        @Test
        @DisplayName("password 없이 소셜 로그인 사용자를 저장할 수 있다")
        void saveSocialUser() {
            User socialUser = User.builder()
                    .loginId("orange")
                    .name("박주형")
                    .email("orange@gmail.com")
                    .socialType(SocialType.GOOGLE)
                    .build();

            User saved = userRepository.save(socialUser);

            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getPassword()).isNull();
            assertThat(saved.getSocialType()).isEqualTo(SocialType.GOOGLE);
            assertThat(saved.isSocialUser()).isTrue();
        }
    }

    @Nested
    @DisplayName("BaseEntity Auditing 테스트")
    class Auditing {

        @Test
        @DisplayName("저장 시 createAt과 updatedAt이 자동으로 설정된다")
        void autoTimestamp() {
            assertThat(savedUser.getCreatedAt()).isNotNull();
            assertThat(savedUser.getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("수정 시 updatedAt이 갱신된다")
        void updateTimestamp() throws InterruptedException {
            var originalUpdatedAt = savedUser.getUpdatedAt();
            Thread.sleep(10);

            savedUser.updateProfile("최경환2", "https://image.com/new.jpg");
            User updated = userRepository.saveAndFlush(savedUser);

            assertThat(updated.getUpdatedAt()).isAfter(originalUpdatedAt);
        }
    }
}