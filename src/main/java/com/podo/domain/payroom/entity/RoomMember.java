package com.podo.domain.payroom.entity;

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
@Table(
        name = "room_members",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_room_user",
                        columnNames = {"room_id", "user_id"}
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoomMember extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private PayRoom payRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    @Builder
    private RoomMember(PayRoom payRoom, User user) {
        this.payRoom = payRoom;
        this.user = user;
        this.joinedAt = LocalDateTime.now();
    }

    public boolean isMemberOf(PayRoom room) {
        return this.payRoom.getId().equals(room.getId());
    }

    public boolean isUser(User user) {
        return this.user.getId().equals(user.getId());
    }
}
