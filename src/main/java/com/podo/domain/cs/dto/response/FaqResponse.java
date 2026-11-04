package com.podo.domain.cs.dto.response;

import com.podo.domain.cs.entity.Faq;
import com.podo.domain.cs.entity.FaqCategory;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FaqResponse {
    private Long id;
    private FaqCategory category;
    private String question;
    private String answer;
    private int displayOrder;

    public static FaqResponse from(Faq faq) {
        return FaqResponse.builder()
                .id(faq.getId())
                .category(faq.getCategory())
                .question(faq.getQuestion())
                .answer(faq.getAnswer())
                .displayOrder(faq.getDisplayOrder())
                .build();
    }
}
