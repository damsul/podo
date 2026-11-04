package com.podo.domain.payroom.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AddMemberRequest {

    @NotBlank(message = "추가할 사용자의 아이디를 입력해주세요")
    private String loginId;

    @Builder
    public AddMemberRequest(String loginId) {
        this.loginId = loginId;
    }
}
