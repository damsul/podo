package com.podo.domain.cs.dto.response;

import com.podo.domain.cs.entity.Inquiry;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AdminInquiryListResponse {
    private Long id;
    private String writerName;
    private String writerLoginId;
    private String title;
    private boolean answered;
    private LocalDateTime createdAt;

    public static AdminInquiryListResponse from(Inquiry inquiry) {
        return AdminInquiryListResponse.builder()
                .id(inquiry.getId())
                .writerName(inquiry.getUser().getName())
                .writerLoginId(inquiry.getUser().getLoginId())
                .title(inquiry.getTitle())
                .answered(inquiry.isAnswered())
                .createdAt(inquiry.getCreatedAt())
                .build();
    }
}
