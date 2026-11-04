package com.podo.domain.cs.dto.response;

import com.podo.domain.cs.entity.Notice;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
// 상세용, 본문 포함
public class NoticeResponse {
    private Long id;
    private String title;
    private String content;
    private boolean pinned;
    private int viewCount;
    private LocalDateTime createdAt;

    public static NoticeResponse from(Notice notice) {
        return NoticeResponse.builder()
                .id(notice.getId())
                .title(notice.getTitle())
                .content(notice.getContent())
                .pinned(notice.isPinned())
                .viewCount(notice.getViewCount())
                .createdAt(notice.getCreatedAt())
                .build();
    }
}
