package com.podo.domain.user.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminUserCreateRequest {

    @NotBlank(message = "이름을 입력해주세요")
    @Size(max = 30, message = "이름은 최대 30자까지 입력 가능합니다")
    private String name;

    @NotBlank(message = "로그인 ID는 필수입니다")
    @Pattern(regexp = "^[a-zA-Z0-9]{4,20}$",
            message = "로그인 ID는 영문과 숫자 4~20자여야 합니다")
    private String loginId;

    @Email(message = "올바른 이메일 형식이 아닙니다")
    private String email;

    @NotBlank(message = "비밀번호는 필수입니다")
    @Size(min = 8, max = 20, message = "비밀번호는 8~20자여야 합니다")
    private String password;

    @NotBlank(message = "비밀번호 확인은 필수입니다")
    private String passwordConfirm;

    public void setEmail(String email) {
        this.email = (email == null || email.isBlank()) ? null : email.trim();
    }

    @AssertTrue(message = "비밀번호가 일치하지 않습니다")
    public boolean isPasswordMatched() {
        return password == null || password.equals(passwordConfirm);
    }
}
