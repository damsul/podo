package com.podo.domain.cs.dto.request;

import com.podo.domain.cs.dto.response.FaqResponse;
import com.podo.domain.cs.entity.FaqCategory;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FaqUpdateRequest {
    @NotNull(message = "카테고리를 선택해주세요.")
    private FaqCategory category;

    @NotBlank(message = "질문을 입력해주세요.")
    @Size(max = 200, message = "질문은 200자 이내로 입력해주세요.")
    private String question;

    @NotBlank(message = "답변을 입력해주세요.")
    @Size(max = 2000, message = "답변은 2000자 이내로 입력해주세요.")
    private String answer;

    @NotNull(message = "노출 순서를 입력해주세요.")
    @Min(value = 0, message = "노출 순서는 0 이상이어야 해요.")
    private Integer displayOrder;

    public static FaqUpdateRequest from(FaqResponse faq) {
        FaqUpdateRequest request = new FaqUpdateRequest();
        request.setCategory(faq.getCategory());
        request.setQuestion(faq.getQuestion());
        request.setAnswer(faq.getAnswer());
        request.setDisplayOrder(faq.getDisplayOrder());
        return request;
    }
}
