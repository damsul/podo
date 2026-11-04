package com.podo.domain.payroom.repository;

import com.podo.domain.payroom.entity.Payment;
import com.podo.domain.payroom.entity.PaymentParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PaymentParticipantRepository extends JpaRepository<PaymentParticipant, Long> {

    List<PaymentParticipant> findByPayment(Payment payment);

    /*@Modifying
    @Query("DELETE FROM PaymentParticipant pp WHERE pp.payment = :payment")
    void deleteByPayment(@Param("payment") Payment payment);*/
}
