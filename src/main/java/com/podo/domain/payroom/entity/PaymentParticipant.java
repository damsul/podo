package com.podo.domain.payroom.entity;

import com.podo.domain.user.entity.User;
import com.podo.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(
        name = "payment_participants",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_payment_user",
                        columnNames = {"payment_id", "user_id"}
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentParticipant extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private boolean settled = false;

    @Builder
    private PaymentParticipant(Payment payment, User user) {
        this.payment = payment;
        this.user = user;
    }

    public void markAsSettled() {
        this.settled = true;
    }

    public void markAsUnsettled() {
        this.settled = false;
    }

    public boolean isUser(User user) {
        return this.user.getId().equals(user.getId());
    }
}
