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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@Slf4j
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

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
    @DisplayName("회원가입 테스트")
    class Signup {

        @Test
        @DisplayName("정상적인 정보로 회원가입에 성공한다")
        void signup_success() {
            User savedUser = signupRequest.toEntity("encoded_password");
            given(userRepository.existsByLoginId("pien")).willReturn(false);
            given(userRepository.existsByEmail("pien@naver.com")).willReturn(false);
            given(passwordEncoder.encode("password123")).willReturn("encoded_password");
            given(userRepository.save(any(User.class))).willReturn(savedUser);

            UserResponse response = userService.signup(signupRequest);

            assertThat(response.getLoginId()).isEqualTo("pien");
            assertThat(response.getName()).isEqualTo("최경환");
            assertThat(response.getEmail()).isEqualTo("pien@naver.com");

            verify(userRepository).existsByLoginId("pien");
            verify(userRepository).existsByEmail("pien@naver.com");
            verify(passwordEncoder).encode("password123");
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("loginId가 중복이면 DuplicateLoginIdException이 발생한다")
        void signup_duplicateLoginId() {
            given(userRepository.existsByLoginId("pien")).willReturn(true);

            assertThatThrownBy(() -> userService.signup(signupRequest))
                    .isInstanceOf(DuplicateLoginIdException.class)
                    .hasMessageContaining("pien");

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("email이 중복이면 DuplicateEmailException이 발생한다")
        void signup_duplicateEmail() {
            given(userRepository.existsByLoginId("pien")).willReturn(false);
            given(userRepository.existsByEmail("pien@naver.com")).willReturn(true);

            assertThatThrownBy(() -> userService.signup(signupRequest))
                    .isInstanceOf(DuplicateEmailException.class)
                    .hasMessageContaining("pien@naver.com");

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("email이 null이어도 회원가입에 성공한다")
        void signup_withoutEmail() {
            SignupRequest requestWithoutEmail = SignupRequest.builder()
                    .loginId("pien")
                    .password("password123")
                    .name("최경환")
                    .build();

            User savedUser = requestWithoutEmail.toEntity("encoded_password");
            given(userRepository.existsByLoginId("pien")).willReturn(false);
            given(passwordEncoder.encode(anyString())).willReturn("encoded_password");
            given(userRepository.save(any(User.class))).willReturn(savedUser);

            UserResponse response = userService.signup(requestWithoutEmail);

            assertThat(response.getEmail()).isNull();

            verify(userRepository, never()).existsByEmail(anyString());
        }
    }

    @Nested
    @DisplayName("사용자 조회 테스트")
    class FindUser {

        @Test
        @DisplayName("ID로 사용자를 조회한다")
        void findById_success() {
            User user = signupRequest.toEntity("encoded_password");
            given(userRepository.findById(1L)).willReturn(Optional.of(user));

            UserResponse response = userService.findById(1L);

            assertThat(response.getLoginId()).isEqualTo("pien");
        }

        @Test
        @DisplayName("존재하지 않는 ID로 조회 시 UserNotFoundException이 발생한다")
        void findById_notFound() {
            given(userRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> userService.findById(999L))
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessageContaining("999");
        }

        @Test
        @DisplayName("loginId로 사용자를 조회한다")
        void findByLoginId_success() {
            User user = signupRequest.toEntity("encoded_password");
            given(userRepository.findByLoginId("pien")).willReturn(Optional.of(user));

            UserResponse response = userService.findByLoginId("pien");

            assertThat(response.getLoginId()).isEqualTo("pien");
        }

        @Test
        @DisplayName("존재하지 않는 loginId로 조회 시 UserNotFoundException이 발생한다")
        void findByLoginId_notFound() {
            given(userRepository.findByLoginId("nonexistent")).willReturn(Optional.empty());

            assertThatThrownBy(() -> userService.findByLoginId("nonexistent"))
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessageContaining("nonexistent");
        }
    }

    @Nested
    @DisplayName("프로필 수정 테스트")
    class UpdateProfile {

        @Test
        @DisplayName("프로필을 수정한다")
        void updateProfile_success() {
            User user = signupRequest.toEntity("encoded_password");
            given(userRepository.findById(1L)).willReturn(Optional.of(user));

            UserResponse response = userService.updateProfile(1L, "최경환2", "https://image.com/new.jpg");

            assertThat(response.getName()).isEqualTo("최경환2");
            assertThat(response.getProfileImageUrl()).isEqualTo("https://image.com/new.jpg");
        }
    }

    @Nested
    @DisplayName("회원 탈퇴 테스트")
    class Withdraw {

        @Test
        @DisplayName("회원 탈퇴를 처리한다")
        void withdraw_success() {
            User user = signupRequest.toEntity("encoded_password");
            given(userRepository.findById(1L)).willReturn(Optional.of(user));

            userService.withdraw(1L);

            assertThat(user.isDeleted()).isTrue();
        }
    }

    @Nested
    @DisplayName("중복 확인 테스트")
    class AvailabilityCheck {

        @Test
        @DisplayName("사용 가능한 loginId면 true를 반환한다")
        void isLoginIdAvailable_true() {
            given(userRepository.existsByLoginId("newuser")).willReturn(false);

            assertThat(userService.isLoginAvailable("newuser")).isTrue();
        }

        @Test
        @DisplayName("이미 사용 중인 loginId면 false를 반환한다")
        void isLoginIdAvailable_false() {
            given(userRepository.existsByLoginId("pien")).willReturn(true);

            assertThat(userService.isLoginAvailable("pien")).isFalse();
        }
    }
}