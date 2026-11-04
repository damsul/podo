package com.podo.domain.payroom.repository;

import com.podo.domain.payroom.entity.PayRoom;
import com.podo.domain.payroom.entity.RoomStatus;
import com.podo.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PayRoomRepository extends JpaRepository<PayRoom, Long> {

    List<PayRoom> findByOwner(User owner);

    List<PayRoom> findByOwnerAndStatus(User owner, RoomStatus status);

    @Query(value = "SELECT p FROM PayRoom p JOIN FETCH p.owner " +
            "WHERE (:status IS NULL OR p.status = :status) " +
            "AND (:keyword IS NULL OR p.title LIKE %:keyword%) " +
            "ORDER BY p.id DESC",
            countQuery = "SELECT COUNT(p) FROM PayRoom p " +
                    "WHERE (:status IS NULL OR p.status = :status) " +
                    "AND (:keyword IS NULL OR p.title LIKE %:keyword%)")
    Page<PayRoom> searchForAdmin(@Param("status") RoomStatus status,
                                 @Param("keyword") String keyword,
                                 Pageable pageable);

}
