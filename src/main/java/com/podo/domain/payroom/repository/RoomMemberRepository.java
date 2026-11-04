package com.podo.domain.payroom.repository;

import com.podo.domain.payroom.entity.PayRoom;
import com.podo.domain.payroom.entity.RoomMember;
import com.podo.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RoomMemberRepository extends JpaRepository<RoomMember, Long> {

    List<RoomMember> findByPayRoom(PayRoom payRoom);

    List<RoomMember> findByUser(User user);

    boolean existsByPayRoomAndUser(PayRoom payRoom, User user);

    long countByPayRoom(PayRoom payRoom);

    @Query("SELECT rm FROM RoomMember rm " +
            "JOIN FETCH rm.payRoom pr " +
            "JOIN FETCH pr.owner " +
            "WHERE rm.user.id = :userId " +
            "ORDER BY pr.createdAt DESC")
    List<RoomMember> findAllByUserIdWithRoom(@Param("userId") Long userId);
}
