package com.podo.domain.cs.dto.response;

import com.podo.domain.cs.entity.Notice;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
// 목록용, 본문 제외
public class NoticeListResponse {
    private Long id;
    private String title;
    private boolean pinned;
    private LocalDateTime createdAt;

    public static NoticeListResponse from(Notice notice) {
        return NoticeListResponse.builder()
                .id(notice.getId())
                .title(notice.getTitle())
                .pinned(notice.isPinned())
                .createdAt(notice.getCreatedAt())
                .build();
    }
}
