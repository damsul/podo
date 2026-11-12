package com.podo.domain.payroom.repository;

import com.podo.domain.payroom.entity.Account;
import com.podo.domain.payroom.entity.PayRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AccountRepository extends JpaRepository<Account, Long> {

    List<Account> findByPayRoom(PayRoom payRoom);

    @Query("""
            select a from Account a
            join fetch a.user
            where a.payRoom = :payRoom
            order by a.id
            """)
    List<Account> findAllByPayRoomWithUser(@Param("payRoom") PayRoom payRoom);

}
