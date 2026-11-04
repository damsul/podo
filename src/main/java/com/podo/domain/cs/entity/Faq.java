package com.podo.domain.cs.entity;

import com.podo.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "faqs")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Faq extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private FaqCategory category;

    @Column(nullable = false, length = 200)
    private String question;

    @Lob
    @Column(nullable = false)
    private String answer;

    @Column(name = "display_order", nullable = false)
    private int displayOrder = 0;

    @Builder
    private Faq(FaqCategory category, String question, String answer, Integer displayOrder) {
        this.category = category;
        this.question = question;
        this.answer = answer;
        if (displayOrder != null) {
            this.displayOrder = displayOrder;
        }
    }

    public void update(FaqCategory category, String question, String answer, int displayOrder) {
        this.category = category;
        this.question = question;
        this.answer = answer;
        this.displayOrder = displayOrder;
    }

    public void changeOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }
}
