package com.podo.domain.user.service;

import com.podo.domain.user.entity.Role;
import com.podo.domain.user.entity.User;
import com.podo.domain.user.repository.UserRepository;
import com.podo.global.storage.ProfileImageStorage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 프로필 사진 교체 시 트랜잭션 결과에 따라 S3 파일이 올바르게 정리되는지 확인
 * - S3는 가짜(MockitoBean)로 바꾸고, 어떤 URL을 지우라고 했는지만 검증한다
 * - 커밋/롤백을 직접 제어해야 하므로 클래스에 @Transactional을 붙이지 않고 TransactionTemplate을 쓴다
 *   (그래서 테스트가 끝나면 @AfterEach에서 직접 데이터를 지운다)
 */
@SpringBootTest
@ActiveProfiles("test")
class UserProfileImageTransactionTest {

    private static final String OLD_URL = "https://test-bucket.s3.ap-northeast-2.amazonaws.com/profiles/1/old.png";
    private static final String NEW_URL = "https://test-bucket.s3.ap-northeast-2.amazonaws.com/profiles/1/new.png";

    @Autowired private UserService userService;
    @Autowired private UserRepository userRepository;
    @Autowired private TransactionTemplate transactionTemplate;

    @MockitoBean
    private ProfileImageStorage profileImageStorage;

    private Long userId;

    @BeforeEach
    void setUp() {
        User user = User.builder().loginId("pien").name("최경환").build();
        user.updateProfile("최경환", OLD_URL);
        userId = userRepository.save(user).getId();

        given(profileImageStorage.upload(eq(userId), any())).willReturn(NEW_URL);
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("커밋되면 이전 사진을 지우고, 새 사진은 남긴다")
    void commit_deletesOldImage() {
        transactionTemplate.executeWithoutResult(status ->
                userService.updateProfileImageForAdmin(Role.USER, userId, image(), false));

        verify(profileImageStorage).deleteIfOwned(OLD_URL);
        verify(profileImageStorage, never()).deleteIfOwned(NEW_URL);
        assertThat(savedImageUrl()).isEqualTo(NEW_URL);
    }

    @Test
    @DisplayName("롤백되면 방금 올린 새 사진을 지우고, 이전 사진은 남긴다")
    void rollback_deletesNewImage() {
        transactionTemplate.executeWithoutResult(status -> {
            userService.updateProfileImageForAdmin(Role.USER, userId, image(), false);
            status.setRollbackOnly();   // 이후 단계에서 실패해 롤백된 상황을 흉내
        });

        verify(profileImageStorage).deleteIfOwned(NEW_URL);
        verify(profileImageStorage, never()).deleteIfOwned(OLD_URL);
        assertThat(savedImageUrl()).isEqualTo(OLD_URL);   // DB는 그대로
    }

    @Test
    @DisplayName("사진 삭제만 하면 업로드 없이 커밋 후 이전 사진을 지운다")
    void deleteOnly_deletesOldImage() {
        transactionTemplate.executeWithoutResult(status ->
                userService.updateProfileImageForAdmin(Role.USER, userId, null, true));

        verify(profileImageStorage, never()).upload(any(), any());
        verify(profileImageStorage).deleteIfOwned(OLD_URL);
        assertThat(savedImageUrl()).isNull();
    }

    private MockMultipartFile image() {
        return new MockMultipartFile("profileImage", "new.png", "image/png", new byte[]{1, 2, 3});
    }

    private String savedImageUrl() {
        return userRepository.findById(userId).orElseThrow().getProfileImageUrl();
    }
}