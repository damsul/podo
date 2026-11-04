package com.podo.domain.cs.dto.response;

import com.podo.domain.cs.entity.Inquiry;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class InquiryResponse {
    private Long id;
    private String title;
    private String content;
    private String answer;
    private boolean answered;
    private LocalDateTime createdAt;
    private LocalDateTime answeredAt;

    public static InquiryResponse from(Inquiry inquiry) {
        return InquiryResponse.builder()
                .id(inquiry.getId())
                .title(inquiry.getTitle())
                .content(inquiry.getContent())
                .answer(inquiry.getAnswer())
                .answered(inquiry.isAnswered())
                .createdAt(inquiry.getCreatedAt())
                .answeredAt(inquiry.getAnsweredAt())
                .build();
    }
}
