package com.podo.domain.cs.entity;

import com.podo.domain.user.entity.User;
import com.podo.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "inquiries")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Inquiry extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 200)
    private String title;

    @Lob
    @Column(nullable = false)
    private String content;

    @Lob
    @Column
    private String answer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InquiryStatus status = InquiryStatus.PENDING;

    @Column(name = "answered_at")
    private LocalDateTime answeredAt;

    @Builder
    private Inquiry(User user, String title, String content) {
        this.user = user;
        this.title = title;
        this.content = content;
    }

    public void registerAnswer(String answer) {
        this.answer = answer;
        this.status = InquiryStatus.ANSWERED;
        this.answeredAt = LocalDateTime.now();
    }

    public void updateContent(String title, String content) {
        if (isAnswered()) {
            throw new IllegalStateException("답변이 완료된 문의는 수정할 수 없습니다");
        }
        this.title = title;
        this.content = content;
    }

    public boolean isWrittenBy(User user) {
        return this.user.getId().equals(user.getId());
    }

    public boolean isAnswered() {
        return this.status == InquiryStatus.ANSWERED;
    }
}
