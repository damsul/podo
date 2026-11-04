package com.podo.domain.user.entity;

import com.podo.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "login_id", unique = true, length = 50)
    private String loginId;

    @Column(length = 100)
    private String password;

    @Column(nullable = false, length = 30)
    private String name;

    @Column(unique = true, length = 100)
    private String email;

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "social_type", length = 20)
    private SocialType socialType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted = false;

    @Builder
    private User(String loginId, String password, String name, String email, String profileImageUrl, SocialType socialType, Role role) {
        this.loginId = loginId;
        this.password = password;
        this.name = name;
        this.email = email;
        this.profileImageUrl = profileImageUrl;
        this.socialType = socialType;
        this.role = role != null ? role : Role.USER;
    }

    // 비즈니스 메서드
    public void updateProfile(String name, String profileImageUrl) {
        this.name = name;
        this.profileImageUrl = profileImageUrl;
    }

    public void updateEmail(String email) {
        this.email = email;
    }

    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    public void softDelete() {
        this.deleted = true;
    }

    public boolean isSocialUser() {
        return this.socialType != null;
    }

}
