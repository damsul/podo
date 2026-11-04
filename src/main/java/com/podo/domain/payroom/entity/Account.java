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
@Table(name = "accounts")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Account extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private PayRoom payRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 30)
    private String bank;

    @Column(name = "account_number", nullable = false, length = 50)
    private String accountNumber;

    @Column(name = "holder_name", nullable = false, length = 30)
    private String holderName;

    @Builder
    private Account(PayRoom payRoom, User user, String bank, String accountNumber, String holderName) {
        this.payRoom = payRoom;
        this.user = user;
        this.bank = bank;
        this.accountNumber = accountNumber;
        this.holderName = holderName;
    }

    public void update(String bank, String accountNumber, String holderName) {
        this.bank = bank;
        this.accountNumber = accountNumber;
        this.holderName = holderName;
    }

    public boolean isOwnedBy(User user) {
        return this.user.getId().equals(user.getId());
    }
}
