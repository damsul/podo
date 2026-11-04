package com.podo.domain.cs.dto.response;

import com.podo.domain.cs.entity.Inquiry;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AdminInquiryResponse {
    private Long id;
    private String writerName;
    private String writerLoginId;
    private String title;
    private String content;
    private String answer;
    private boolean answered;
    private LocalDateTime createdAt;
    private LocalDateTime answeredAt;

    public static AdminInquiryResponse from(Inquiry inquiry) {
        return AdminInquiryResponse.builder()
                .id(inquiry.getId())
                .writerName(inquiry.getUser().getName())
                .writerLoginId(inquiry.getUser().getLoginId())
                .title(inquiry.getTitle())
                .content(inquiry.getContent())
                .answer(inquiry.getAnswer())
                .answered(inquiry.isAnswered())
                .createdAt(inquiry.getCreatedAt())
                .answeredAt(inquiry.getAnsweredAt())
                .build();
    }
}
