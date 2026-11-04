package com.podo.domain.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProfileEditRequest {
    private String password;
    private String passwordConfirm;

    @Email(message = "올바른 이메일 형식이 아니에요")
    @Size(max = 100, message = "이메일은 100자 이하로 입력해주세요")
    private String email;

    private boolean deleteProfileImage;
}
