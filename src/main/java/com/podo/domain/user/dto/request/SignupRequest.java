package com.podo.domain.user.dto.request;

import com.podo.domain.user.entity.Role;
import com.podo.domain.user.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SignupRequest {

    @NotBlank(message = "로그인 ID는 필수입니다")
    @Pattern(regexp = "^[a-zA-Z0-9]{4,20}$",
            message = "로그인 ID는 영문과 숫자 4~20자여야 합니다")
    private String loginId;

    @NotBlank(message = "비밀번호는 필수입니다")
    @Size(min = 8, max = 20, message = "비밀번호는 8~20자여야 합니다")
    private String password;

    @NotBlank(message = "이름은 필수입니다")
    @Size(max = 30, message = "이름은 최대 30자까지 입력 가능합니다")
    private String name;

    @Email(message = "올바른 이메일 형식이 아닙니다")
    private String email; // 선택 가능 (소셜 가입 시 없을 수 있음)

    @Builder
    public SignupRequest(String loginId, String password, String name, String email) {
        this.loginId = loginId;
        this.password = password;
        this.name = name;
        this.email = email;
    }


    /**
     * DTO -> Entity 변환 메서드
     * password는 외부에서 암호화된 값을 전달받음
     */
    public User toEntity(String encodedPassword) {
        return User.builder()
                .loginId(this.loginId)
                .password(encodedPassword)
                .name(this.name)
                .email(this.email)
                .role(Role.USER)
                .build();
    }
}
