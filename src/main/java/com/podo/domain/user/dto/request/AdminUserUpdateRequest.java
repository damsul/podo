package com.podo.domain.user.dto.request;

import com.podo.domain.user.dto.response.AdminUserDetailResponse;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminUserUpdateRequest {

    @NotBlank(message = "이름은 필수입니다")
    @Size(max = 30, message = "이름은 최대 30자까지 입력 가능합니다")
    private String name;

    @Email(message = "올바른 이메일 형식이 아닙니다")
    private String email;

    public void setEmail(String email) {
        this.email = (email == null || email.isBlank()) ? null : email.trim();
    }

    public static AdminUserUpdateRequest from(AdminUserDetailResponse user) {
        AdminUserUpdateRequest form = new AdminUserUpdateRequest();
        form.setName(user.getName());
        form.setEmail(user.getEmail());
        return form;
    }
}
