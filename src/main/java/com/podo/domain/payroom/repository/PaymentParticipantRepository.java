package com.podo.domain.payroom.repository;

import com.podo.domain.payroom.entity.Payment;
import com.podo.domain.payroom.entity.PaymentParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public interface PaymentParticipantRepository extends JpaRepository<PaymentParticipant, Long> {

    List<PaymentParticipant> findByPayment(Payment payment);

    // 여러 결제의 참여자를 한 번에 조회 (참여자 사용자까지 함께) → 결제마다 조회하던 N+1 제거
    @Query("""
            select pp from PaymentParticipant pp
            join fetch pp.user
            where pp.payment in :payments
            order by pp.id
            """)
    List<PaymentParticipant> findAllByPaymentInWithUser(@Param("payments") List<Payment> payments);

    // 결제 id별로 묶어서 돌려준다. 결제가 없으면 쿼리를 보내지 않는다
    default Map<Long, List<PaymentParticipant>> findGroupedByPayment(List<Payment> payments) {
        if (payments.isEmpty()) {
            return Map.of();
        }
        return findAllByPaymentInWithUser(payments).stream()
                .collect(Collectors.groupingBy(pp -> pp.getPayment().getId()));
    }
}