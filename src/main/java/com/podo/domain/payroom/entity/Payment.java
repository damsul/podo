package com.podo.domain.payroom.entity;

import com.podo.domain.user.entity.User;
import com.podo.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Getter
@Table(name = "payments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private PayRoom payRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payer_id", nullable = false)
    private User payer;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false)
    private int amount;

    @Column(name = "paid_at", nullable = false)
    private LocalDate paidAt;

    @Builder
    private Payment(PayRoom payRoom, User payer, String title, int amount, LocalDate paidAt) {
        validateAmount(amount);
        this.payRoom = payRoom;
        this.payer = payer;
        this.title = title;
        this.amount = amount;
        this.paidAt = paidAt != null ? paidAt : LocalDate.now();
    }

    public void update(String title, int amount, LocalDate paidAt) {
        validateAmount(amount);
        this.title = title;
        this.amount = amount;
        this.paidAt = paidAt != null ? paidAt : LocalDate.now();
    }

    public boolean isPaidBy(User user) {
        return this.payer.getId().equals(user.getId());
    }

    private void validateAmount(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("결제 금액은 0보다 커야 합니다.");
        }
    }
}
