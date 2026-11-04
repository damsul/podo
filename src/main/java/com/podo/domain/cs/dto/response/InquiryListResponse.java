package com.podo.domain.cs.dto.response;

import com.podo.domain.cs.entity.Inquiry;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class InquiryListResponse {
    private Long id;
    private String title;
    private boolean answered;
    private LocalDateTime createdAt;

    public static InquiryListResponse from(Inquiry inquiry) {
        return InquiryListResponse.builder()
                .id(inquiry.getId())
                .title(inquiry.getTitle())
                .answered(inquiry.isAnswered())
                .createdAt(inquiry.getCreatedAt())
                .build();
    }
}
