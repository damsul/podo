package com.podo.domain.user.service;

import com.podo.domain.payroom.entity.RoomMember;
import com.podo.domain.payroom.repository.RoomMemberRepository;
import com.podo.domain.user.dto.request.*;
import com.podo.domain.user.dto.response.AdminUserDetailResponse;
import com.podo.domain.user.dto.response.AdminUserListResponse;
import com.podo.domain.user.dto.response.UserResponse;
import com.podo.domain.user.entity.Role;
import com.podo.domain.user.entity.User;
import com.podo.domain.user.exception.*;
import com.podo.domain.user.repository.UserRepository;
import com.podo.global.storage.ProfileImageStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoomMemberRepository roomMemberRepository;
    private final ProfileImageStorage profileImageStorage;

    @Transactional
    public UserResponse signup(SignupRequest request) {
        validateDuplicateLoginId(request.getLoginId());
        validateDuplicateEmail(request.getEmail());

        String encodedPassword = passwordEncoder.encode(request.getPassword());
        User user = request.toEntity(encodedPassword);
        User savedUser = userRepository.save(user);

        return UserResponse.from(savedUser);
    }

    public UserResponse findById(Long id) {
        User user = findUserById(id);
        return UserResponse.from(user);
    }

    public UserResponse findByLoginId(String loginId) {
        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new UserNotFoundException(loginId));
        return UserResponse.from(user);
    }

    //TODO 제거?
    @Transactional
    public UserResponse updateProfile(Long userId, String name, String profileImageUrl) {
        User user = findUserById(userId);
        user.updateProfile(name, profileImageUrl);
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse updateMyProfile(Long userId, ProfileEditRequest request, String uploadedImageUrl) {
        User user = findUserById(userId);

        if (uploadedImageUrl != null) {
            user.updateProfile(user.getName(), uploadedImageUrl);
        }

        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            validateDuplicateEmail(request.getEmail());
            user.updateEmail(request.getEmail());
        }

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            if (user.isSocialUser()) {
                throw new SocialUserPasswordChangeException();
            }
            user.changePassword(passwordEncoder.encode(request.getPassword()));
        }

        return UserResponse.from(user);
    }

    public UserResponse getMyPage(Long userId) {
        User user = findUserById(userId);
        return UserResponse.from(user);
    }

    @Transactional
    public void withdraw(Long userId) {
        User user = findUserById(userId);
        user.softDelete();
    }

    // 중복 확인 (회원가입 폼에서 실시간 검증용)
    public boolean isLoginAvailable(String loginId) {
        return !userRepository.existsByLoginId(loginId);
    }

    public boolean isEmailAvailable(String email) {
        return !userRepository.existsByEmail(email);
    }

    public List<UserResponse> search(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }

        return userRepository.searchByKeyword(keyword).stream()
                .map(UserResponse::from)
                .toList();
    }

    @Transactional
    public UserResponse updateMyProfile(Long userId, ProfileEditRequest request, MultipartFile profileImage) {
        User user = findUserById(userId);

        if (StringUtils.hasText(request.getEmail()) && !request.getEmail().equals(user.getEmail())) {
            validateDuplicateEmail(request.getEmail());
            user.updateEmail(request.getEmail());
        }

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            if (user.isSocialUser()) {
                throw new SocialUserPasswordChangeException();
            }
            user.changePassword(passwordEncoder.encode(request.getPassword()));
        }

        applyProfileImage(user, profileImage, request.isDeleteProfileImage());

        return UserResponse.from(user);
    }

    // ==== 관리자용 ====
    public Page<AdminUserListResponse> findAllForAdmin(Role role, Boolean deleted, String keyword, Pageable pageable) {
        String normalized = (keyword == null) ? "" : keyword.trim();
        return userRepository.searchForAdmin(role, deleted, normalized, pageable)
                .map(AdminUserListResponse::of);
    }

    public AdminUserDetailResponse getDetailForAdmin(Role role, Long id) {
        User user = findUserForAdmin(role, id);
        List<RoomMember> memberships = (role == Role.USER)
                ? roomMemberRepository.findAllByUserIdWithRoom(id)
                : List.of();
        return AdminUserDetailResponse.of(user, memberships);
    }

    @Transactional
    public Long createForAdmin(Role role, AdminUserCreateRequest request) {
        validateDuplicateLoginId(request.getLoginId());
        validateDuplicateEmail(request.getEmail());

        User user = User.builder()
                .loginId(request.getLoginId())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .email(request.getEmail())
                .role(role)
                .build();
        return userRepository.save(user).getId();
    }

    @Transactional
    public void updateForAdmin(Role role, Long id, AdminUserUpdateRequest request) {
        User user = findUserForAdmin(role, id);
        if (!Objects.equals(user.getEmail(), request.getEmail())) {
            validateDuplicateEmail(request.getEmail());
            user.updateEmail(request.getEmail());
        }
        user.updateProfile(request.getName(), user.getProfileImageUrl());
    }

    @Transactional
    public void changePasswordForAdmin(Role role, Long id, AdminPasswordChangeRequest request) {
        User user = findUserForAdmin(role, id);
        if (user.isSocialUser()) {
            throw new SocialUserPasswordChangeException();
        }
        user.changePassword(passwordEncoder.encode(request.getPassword()));
    }

    // 관리자 계정 탈퇴: 본인 계정은 막는다 (활성 관리자가 최소 1명 남도록)
    @Transactional
    public void withdrawForAdmin(Role role, Long id) {
        findUserForAdmin(role, id).softDelete();
    }

    // 화면별 대상 role만 조회: 사용자 화면에서 관리자 id를 넣거나, 그 반대로 접근해도 못 찾게 막는다
    @Transactional
    public void withdrawAdminAccount(Long id, Long actorId) {
        if (id.equals(actorId)) {
            throw new SelfWithdrawException();
        }
        withdrawForAdmin(Role.ADMIN, id);
    }

    @Transactional
    public void updateProfileImageForAdmin(Role role, Long id, MultipartFile file, boolean delete) {
        applyProfileImage(findUserForAdmin(role, id), file, delete);
    }


    // private 헬퍼 메서드
    private void validateDuplicateLoginId(String loginId) {
        if (userRepository.existsByLoginId(loginId)) {
            throw new DuplicateLoginIdException(loginId);
        }
    }

    private void validateDuplicateEmail(String email) {
        if (email == null) return;
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateEmailException(email);
        }
    }

    private User findUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    // 관리자 화면은 일반 사용자(Role.USER)만 다루므로 ADMIN 계정 id를 URL로 넣어도 못 찾게 막는다
    private User findUserForAdmin(Role role, Long id) {
        return userRepository.findById(id)
                .filter(u -> u.getRole() == role)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    private void applyProfileImage(User user, MultipartFile file, boolean delete) {
        boolean hasNew = file != null && !file.isEmpty();
        if (!hasNew && !delete) {
            return;
        }
        String oldUrl = user.getProfileImageUrl();
        String newUrl = hasNew ? profileImageStorage.upload(user.getId(), file) : null;
        user.updateProfile(user.getName(), newUrl);

        if (oldUrl != null) {
            // DB 커밋이 성공한 뒤에만 이전 파일 삭제 (롤백됐는데 사지만 사라지는 걸 방비)
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    profileImageStorage.deleteIfOwned(oldUrl);
                }
            });
        }
    }
}
