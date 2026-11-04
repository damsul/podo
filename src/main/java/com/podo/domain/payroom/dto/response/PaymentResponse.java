package com.podo.domain.payroom.dto.response;

import com.podo.domain.payroom.entity.Payment;
import com.podo.domain.payroom.entity.PaymentParticipant;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class PaymentResponse {

    private Long id;
    private Long roomId;
    private Long payerId;
    private String payerName;
    private String title;
    private int amount;
    private LocalDate paidAt;
    private List<ParticipantInfo> participants;

    public static PaymentResponse from(Payment payment, List<PaymentParticipant> participants) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .roomId(payment.getPayRoom().getId())
                .payerId(payment.getPayer().getId())
                .payerName(payment.getPayer().getName())
                .title(payment.getTitle())
                .amount(payment.getAmount())
                .paidAt(payment.getPaidAt())
                .participants(participants.stream()
                        .map(ParticipantInfo::from)
                        .toList())
                .build();
    }

    @Getter
    @Builder
    public static class ParticipantInfo {
        private Long userId;
        private String userName;
        private boolean settled;

        public static ParticipantInfo from(PaymentParticipant pp) {
            return ParticipantInfo.builder()
                    .userId(pp.getUser().getId())
                    .userName(pp.getUser().getName())
                    .settled(pp.isSettled())
                    .build();
        }
    }
}
