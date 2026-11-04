package com.podo.domain.cs.entity;

import com.podo.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "notices")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notice extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Lob
    @Column(nullable = false)
    private String content;

    @Column(name = "is_pinned", nullable = false)
    private boolean pinned = false;

    @Column(name = "view_count", nullable = false)
    private int viewCount = 0;

    @Builder
    private Notice(String title, String content, Boolean pinned) {
        this.title = title;
        this.content = content;
        if (pinned != null) {
            this.pinned = pinned;
        }
    }

    public void update(String title, String content, boolean pinned) {
        this.title = title;
        this.content = content;
        this.pinned = pinned;
    }

    public void pin() {
        this.pinned = true;
    }

    public void unpin() {
        this.pinned = false;
    }

    public void increaseViewCount() {
        this.viewCount++;
    }
}