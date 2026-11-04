package com.podo.domain.payroom.entity;

import com.podo.domain.user.entity.User;
import com.podo.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Getter
@Table(name = "pay_rooms")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PayRoom extends BaseEntity {
    private static final Set<Integer> VALID_ROUND_UNITS = Set.of(1, 10, 100, 1000);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false, length = 50)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RoomStatus status = RoomStatus.IN_PROGRESS;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "round_unit", nullable = false)
    private int roundUnit = 10;

    @Builder
    private PayRoom(User owner, String title, LocalDate dueDate, Integer roundUnit) {
        this.owner = owner;
        this.title = title;
        this.dueDate = dueDate;
        if (roundUnit != null) {
            validateRoundUnit(roundUnit);
            this.roundUnit = roundUnit;
        }
    }

    public void updateInfo(String title, LocalDate dueDate, Integer roundUnit) {
        this.title = title;
        this.dueDate = dueDate;
        if (roundUnit != null) {
            validateRoundUnit(roundUnit);
            this.roundUnit = roundUnit;
        }
    }

    private void validateRoundUnit(int roundUnit) {
        if (!VALID_ROUND_UNITS.contains(roundUnit)) {
            throw new IllegalArgumentException("절사 단위는 1, 10, 100, 1000원만 가능합니다");
        }
    }

    public void complete() {
        this.status = RoomStatus.COMPLETED;
    }

    public void reopen() {
        this.status = RoomStatus.IN_PROGRESS;
    }

    public boolean isOwner(User user) {
        return this.owner.getId().equals(user.getId());
    }
}
