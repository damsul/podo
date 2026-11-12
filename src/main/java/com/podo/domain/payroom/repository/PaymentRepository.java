package com.podo.domain.payroom.repository;

import com.podo.domain.payroom.entity.PayRoom;
import com.podo.domain.payroom.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByPayRoom(PayRoom payRoom);

    @Query("""
            select p from Payment p
            join fetch p.payer
            where p.payRoom = :payRoom
            order by p.id
           """)
    List<Payment> findAllByPayRoomWithPayer(@Param("payRoom") PayRoom payRoom);

}
