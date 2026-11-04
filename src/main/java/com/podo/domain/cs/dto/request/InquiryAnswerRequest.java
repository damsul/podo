package com.podo.domain.cs.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InquiryAnswerRequest {
    @NotBlank(message = "답변 내용을 입력해주세요.")
    private String answer;
}
