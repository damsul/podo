package com.podo.domain.user.service;

import com.podo.domain.user.dto.request.SignupRequest;
import com.podo.domain.user.dto.response.UserResponse;
import com.podo.domain.user.entity.User;
import com.podo.domain.user.exception.DuplicateEmailException;
import com.podo.domain.user.exception.DuplicateLoginIdException;
import com.podo.domain.user.exception.UserNotFoundException;
import com.podo.domain.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Slf4j
public class UserServiceIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private SignupRequest signupRequest;

    @BeforeEach
    void setUp() {
        signupRequest = SignupRequest.builder()
                .loginId("pien")
                .password("password123")
                .name("최경환")
                .email("pien@naver.com")
                .build();
    }

    @Nested
    @DisplayName("회원가입 통합 테스트")
    class Signup {
        @Test
        @DisplayName("회원가입에 성공하면 DB에 저장되고 비밀번호가 BCrypt로 암호화된다")
        void signup_success() {
            UserResponse response = userService.signup(signupRequest);

            assertThat(response.getId()).isNotNull();
            assertThat(response.getLoginId()).isEqualTo("pien");
            assertThat(response.getName()).isEqualTo("최경환");

            User savedUser = userRepository.findById(response.getId()).orElseThrow();
            assertThat(savedUser.getLoginId()).isEqualTo("pien");

            assertThat(savedUser.getPassword())
                    .isNotEqualTo("password123")
                    .startsWith("$2a$");

            assertThat(passwordEncoder.matches("password123", savedUser.getPassword()))
                    .isTrue();
        }

        @Test
        @DisplayName("중복된 loginId로 가입 시 예외 발생 + DB에 저장되지 않는다")
        void signup_duplicateLoginId() {
            userService.signup(signupRequest);

            SignupRequest duplicateRequest = SignupRequest.builder()
                    .loginId("pien")
                    .password("anotherPassword")
                    .name("다른사람")
                    .email("other@email.com")
                    .build();

            assertThatThrownBy(() -> userService.signup(duplicateRequest))
                    .isInstanceOf(DuplicateLoginIdException.class);

            long count = userRepository.count();
            assertThat(count).isEqualTo(1);

        }

        @Test
        @DisplayName("중복된 이메일로 가입 시 예외 발생")
        void signup_duplicateEmail() {
            userService.signup(signupRequest);

            SignupRequest duplicateEmailRequest = SignupRequest.builder()
                    .loginId("different_id")
                    .password("password123")
                    .name("다른사람")
                    .email("pien@naver.com")
                    .build();

            assertThatThrownBy(() -> userService.signup(duplicateEmailRequest))
                    .isInstanceOf(DuplicateEmailException.class);
        }

        @Test
        @DisplayName("이메일 없이도 가입할 수 있다")
        void signup_withoutEmail() {
            SignupRequest noEmailRequest = SignupRequest.builder()
                    .loginId("noemail")
                    .password("password123")
                    .name("이메일 없음")
                    .build();

            UserResponse response = userService.signup(noEmailRequest);

            assertThat(response.getEmail()).isNull();

            User savedUser = userRepository.findById(response.getId()).orElseThrow();
            assertThat(savedUser.getEmail()).isNull();
        }
    }

    @Nested
    @DisplayName("프로필 수정 통합 테스트")
    class UpdateProfile {

        @Test
        @DisplayName("프로필 수정 시 JPA 변경 감지로 DB에 자동 반영된다")
        void updateProfile_dirtyChecking() {
            UserResponse signedUp = userService.signup(signupRequest);
            Long userId = signedUp.getId();

            userService.updateProfile(userId, "최경환2", "https://image.com/new.jpg");

            User updated = userRepository.findById(userId).orElseThrow();
            assertThat(updated.getName()).isEqualTo("최경환2");
            assertThat(updated.getProfileImageUrl()).isEqualTo("https://image.com/new.jpg");
        }
    }

    @Nested
    @DisplayName("회원 탈퇴 통합 테스트")
    class Withdraw {

        @Test
        @DisplayName("회원 탈퇴 시 DB에서 delete가 true로 변경된다 (soft delete)")
        void withdraw_softDelete() {
            UserResponse signedUp = userService.signup(signupRequest);
            Long userId = signedUp.getId();

            userService.withdraw(userId);

            User withdraw = userRepository.findById(userId).orElseThrow();
            assertThat(withdraw.isDeleted()).isTrue();

            assertThat(withdraw.getLoginId()).isEqualTo("pien");
            assertThat(withdraw.getName()).isEqualTo("최경환");
        }
    }

    @Nested
    @DisplayName("사용자 조회 통합 테스트")
    class FindUser {

        @Test
        @DisplayName("존재하지 않는 ID로 조회 시 UserNotFoundException 발생")
        void findById_notFound() {
            assertThatThrownBy(() -> userService.findById(99999L))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }

    @Test
    @DisplayName("같은 비밀번호라도 매번 다른 해시가 생성된다 (BCrypt Salt)")
    void bcryptSaltDemo() {
        String password = "password123";

        String hash1 = passwordEncoder.encode(password);
        String hash2 = passwordEncoder.encode(password);
        String hash3 = passwordEncoder.encode(password);

        log.info("=========== BCrypt Salt 시연 ===========");
        log.info("원본 비밀번호: {}", password);
        log.info("암호화 결과 1: {}", hash1);
        log.info("암호화 결과 2: {}", hash2);
        log.info("암호화 결과 3: {}", hash3);
        log.info("======================================");

        assertThat(hash1).isNotEqualTo(hash2);
        assertThat(hash2).isNotEqualTo(hash3);

        assertThat(passwordEncoder.matches(password, hash1)).isTrue();
        assertThat(passwordEncoder.matches(password, hash2)).isTrue();
        assertThat(passwordEncoder.matches(password, hash3)).isTrue();
    }
}
