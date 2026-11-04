package com.podo.domain.payroom.service;

import com.podo.domain.payroom.dto.response.SettlementResponse;
import com.podo.domain.payroom.dto.response.SettlementResponse.TransferInfo;
import com.podo.domain.payroom.entity.PayRoom;
import com.podo.domain.payroom.entity.Payment;
import com.podo.domain.payroom.entity.PaymentParticipant;
import com.podo.domain.payroom.exception.PayRoomNotFoundException;
import com.podo.domain.payroom.repository.PayRoomRepository;
import com.podo.domain.payroom.repository.PaymentParticipantRepository;
import com.podo.domain.payroom.repository.PaymentRepository;
import com.podo.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class SettlementServiceTest {

    @Mock
    private PayRoomRepository payRoomRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentParticipantRepository participantRepository;

    @InjectMocks
    private SettlementService settlementService;

    private User gyounghwan;
    private User minwook;
    private User juhwang;
    private User youngjun;
    private User sohyun;
    private PayRoom payRoom;

    @BeforeEach
    void setUp() {
        gyounghwan = createUserWithId(1L, "경환");
        minwook = createUserWithId(2L, "민욱");
        juhwang = createUserWithId(3L, "주황");
        youngjun = createUserWithId(4L, "영준");
        sohyun = createUserWithId(5L, "소현");

        payRoom = createPayRoomWithId(10L, PayRoom.builder()
                .owner(gyounghwan)
                .title("Podo MT")
                .roundUnit(10)
                .build());
    }

    @Nested
    @DisplayName("단일 결제 정산 테스트")
    class SinglePayment {

        @Test
        @DisplayName("80,000원을 5명이 균등 분할한다 - 1인당 16,000원")
        void calculate_evenSplit() {
            Payment payment = createPayment(100L, gyounghwan, "장보기", 80000);
            List<PaymentParticipant> participants = List.of(
                    createParticipant(1L, payment, gyounghwan),
                    createParticipant(2L, payment, minwook),
                    createParticipant(3L, payment, juhwang),
                    createParticipant(4L, payment, youngjun),
                    createParticipant(5L, payment, sohyun)
            );

            given(payRoomRepository.findById(10L)).willReturn(Optional.of(payRoom));
            given(paymentRepository.findByPayRoom(payRoom)).willReturn(List.of(payment));
            given(participantRepository.findByPayment(payment)).willReturn(participants);

            SettlementResponse response = settlementService.calculate(10L);

            assertThat(response.getRoomTitle()).isEqualTo("Podo MT");
            assertThat(response.getTotalAmount()).isEqualTo(80000);

            assertThat(response.getTransfers()).hasSize(4);

            assertThat(response.getTransfers()).allMatch(t -> t.getAmount() == 16000);

            assertThat(response.getTransfers())
                    .allMatch(t -> t.getToUserId().equals(gyounghwan.getId()));
            assertThat(response.getTransfers())
                    .allMatch(t -> t.getToUserName().equals("경환"));

            assertThat(response.getTransfers())
                    .noneMatch(t -> t.getFromUserId().equals(gyounghwan.getId()));
        }

        @Test
        @DisplayName("결제자 본인은 송금 대상에서 제외된다")
        void calculate_payerExcluded() {
            Payment payment = createPayment(100L, gyounghwan, "장보기", 80000);
            List<PaymentParticipant> participants = List.of(
                    createParticipant(1L, payment, gyounghwan),
                    createParticipant(2L, payment, minwook),
                    createParticipant(3L, payment, juhwang)
            );

            given(payRoomRepository.findById(10L)).willReturn(Optional.of(payRoom));
            given(paymentRepository.findByPayRoom(payRoom)).willReturn(List.of(payment));
            given(participantRepository.findByPayment(payment)).willReturn(participants);

            SettlementResponse response = settlementService.calculate(10L);

            assertThat(response.getTransfers()).hasSize(2);
            assertThat(response.getTransfers())
                    .extracting(TransferInfo::getFromUserName)
                    .containsExactlyInAnyOrder("민욱", "주황");
        }
    }

    @Nested
    @DisplayName("절사 옵션 테스트")
    class Rounding {

        @Test
        @DisplayName("10원 단위 절사 - 20,000원 ÷ 3 = 6,666.67 → 6,660원")
        void calculate_round10() {
            payRoom = createPayRoomWithId(10L, PayRoom.builder()
                    .owner(sohyun)
                    .title("야식")
                    .roundUnit(10)
                    .build());

            Payment payment = createPayment(100L, sohyun, "야식", 20000);
            List<PaymentParticipant> participants = List.of(
                    createParticipant(1L, payment, sohyun),
                    createParticipant(2L, payment, gyounghwan),
                    createParticipant(3L, payment, youngjun)
            );

            given(payRoomRepository.findById(10L)).willReturn(Optional.of(payRoom));
            given(paymentRepository.findByPayRoom(payRoom)).willReturn(List.of(payment));
            given(participantRepository.findByPayment(payment)).willReturn(participants);

            SettlementResponse response = settlementService.calculate(10L);

            assertThat(response.getTransfers()).hasSize(2);
            assertThat(response.getTransfers())
                    .allMatch(t -> t.getAmount() == 6660);
        }

        @Test
        @DisplayName("100원 단위 절사 - 20,000원 ÷ 3 = 6,666.67 → 6,600원")
        void calculate_round100() {
            payRoom = createPayRoomWithId(10L, PayRoom.builder()
                    .owner(sohyun)
                    .title("야식")
                    .roundUnit(100)
                    .build());

            Payment payment = createPayment(100L, sohyun, "야식", 20000);
            List<PaymentParticipant> participants = List.of(
                    createParticipant(1L, payment, sohyun),
                    createParticipant(2L, payment, gyounghwan),
                    createParticipant(3L, payment, youngjun)
            );

            given(payRoomRepository.findById(10L)).willReturn(Optional.of(payRoom));
            given(paymentRepository.findByPayRoom(payRoom)).willReturn(List.of(payment));
            given(participantRepository.findByPayment(payment)).willReturn(participants);

            SettlementResponse response = settlementService.calculate(10L);

            assertThat(response.getTransfers())
                    .allMatch(t -> t.getAmount() == 6600);  // 6666.67 → 6600 (100원 절사)
        }

        @Test
        @DisplayName("1000원 단위 절사 - 20,000원 ÷ 3 = 6,666.67 → 6,000원")
        void calculate_round1000() {
            payRoom = createPayRoomWithId(10L, PayRoom.builder()
                    .owner(sohyun)
                    .title("야식")
                    .roundUnit(1000)
                    .build());

            Payment payment = createPayment(100L, sohyun, "야식", 20000);
            List<PaymentParticipant> participants = List.of(
                    createParticipant(1L, payment, sohyun),
                    createParticipant(2L, payment, gyounghwan),
                    createParticipant(3L, payment, youngjun)
            );

            given(payRoomRepository.findById(10L)).willReturn(Optional.of(payRoom));
            given(paymentRepository.findByPayRoom(payRoom)).willReturn(List.of(payment));
            given(participantRepository.findByPayment(payment)).willReturn(participants);

            // when
            SettlementResponse response = settlementService.calculate(10L);

            // then
            assertThat(response.getTransfers())
                    .allMatch(t -> t.getAmount() == 6000);  // 6666.67 → 6000 (1000원 절사)
        }

        @Test
        @DisplayName("절사할 게 없는 경우 - 80,000원 ÷ 5 = 정확히 16,000원")
        void calculate_noRoundingNeeded() {
            Payment payment = createPayment(100L, gyounghwan, "장보기", 80000);
            List<PaymentParticipant> participants = List.of(
                    createParticipant(1L, payment, gyounghwan),
                    createParticipant(2L, payment, minwook),
                    createParticipant(3L, payment, juhwang),
                    createParticipant(4L, payment, youngjun),
                    createParticipant(5L, payment, sohyun)
            );

            given(payRoomRepository.findById(10L)).willReturn(Optional.of(payRoom));
            given(paymentRepository.findByPayRoom(payRoom)).willReturn(List.of(payment));
            given(participantRepository.findByPayment(payment)).willReturn(participants);

            SettlementResponse response = settlementService.calculate(10L);

            assertThat(response.getTransfers())
                    .allMatch(t -> t.getAmount() == 16000);
        }
    }

    @Nested
    @DisplayName("복수 결제 정산 테스트")
    class MultiplePayments {

        @Test
        @DisplayName("여러 결제가 있을 때 모든 송금이 합쳐서 계산된다")
        void calculate_multiplePayments() {
            Payment payment1 = createPayment(100L, gyounghwan, "장보기", 80000);
            Payment payment2 = createPayment(101L, sohyun, "야식", 20000);

            List<PaymentParticipant> participants1 = List.of(
                    createParticipant(1L, payment1, gyounghwan),
                    createParticipant(2L, payment1, minwook),
                    createParticipant(3L, payment1, juhwang),
                    createParticipant(4L, payment1, youngjun),
                    createParticipant(5L, payment1, sohyun)
            );

            List<PaymentParticipant> participants2 = List.of(
                    createParticipant(6L, payment2, sohyun),
                    createParticipant(7L, payment2, gyounghwan),
                    createParticipant(8L, payment2, youngjun)
            );

            given(payRoomRepository.findById(10L)).willReturn(Optional.of(payRoom));
            given(paymentRepository.findByPayRoom(payRoom))
                    .willReturn(List.of(payment1, payment2));
            given(participantRepository.findByPayment(payment1)).willReturn(participants1);
            given(participantRepository.findByPayment(payment2)).willReturn(participants2);

            SettlementResponse response = settlementService.calculate(10L);

            assertThat(response.getTotalAmount()).isEqualTo(100000);  // 80000 + 20000

            assertThat(response.getTransfers()).hasSize(6);

            long groceryTransfers = response.getTransfers().stream()
                    .filter(t -> t.getPaymentTitle().equals("장보기"))
                    .count();
            assertThat(groceryTransfers).isEqualTo(4);

            long lateNightTransfers = response.getTransfers().stream()
                    .filter(t -> t.getPaymentTitle().equals("야식"))
                    .count();
            assertThat(lateNightTransfers).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("엣지 케이스 테스트")
    class EdgeCases {

        @Test
        @DisplayName("존재하지 않는 방 ID로 정산 요청 시 예외 발생")
        void calculate_roomNotFound() {
            given(payRoomRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> settlementService.calculate(999L))
                    .isInstanceOf(PayRoomNotFoundException.class);
        }

        @Test
        @DisplayName("결제가 없는 방의 정산 결과 - 송금 0건")
        void calculate_emptyRoom() {
            given(payRoomRepository.findById(10L)).willReturn(Optional.of(payRoom));
            given(paymentRepository.findByPayRoom(payRoom)).willReturn(List.of());

            SettlementResponse response = settlementService.calculate(10L);

            assertThat(response.getTotalAmount()).isZero();
            assertThat(response.getTransfers()).isEmpty();
        }

        @Test
        @DisplayName("참여자가 결제자 1명뿐인 경우 - 송금 0건")
        void calculate_onlyPayerAsParticipant() {
            Payment payment = createPayment(100L, gyounghwan, "혼자 야식", 10000);
            List<PaymentParticipant> participants = List.of(
                    createParticipant(1L, payment, gyounghwan)
            );

            given(payRoomRepository.findById(10L)).willReturn(Optional.of(payRoom));
            given(paymentRepository.findByPayRoom(payRoom)).willReturn(List.of(payment));
            given(participantRepository.findByPayment(payment)).willReturn(participants);

            SettlementResponse response = settlementService.calculate(10L);

            assertThat(response.getTransfers()).isEmpty();
            assertThat(response.getTotalAmount()).isEqualTo(10000);
        }
    }

    @Nested
    @DisplayName("송금 정보 상세 검증")
    class TransferInfoDetails {

        @Test
        @DisplayName("송금 정보에 결제 정보와 참여자 ID가 포함된다")
        void calculate_transferContainsAllInfo() {
            Payment payment = createPayment(100L, gyounghwan, "장보기", 80000);
            PaymentParticipant minwookParticipant = createParticipant(2L, payment, minwook);

            List<PaymentParticipant> participants = List.of(
                    createParticipant(1L, payment, gyounghwan),
                    minwookParticipant
            );

            given(payRoomRepository.findById(10L)).willReturn(Optional.of(payRoom));
            given(paymentRepository.findByPayRoom(payRoom)).willReturn(List.of(payment));
            given(participantRepository.findByPayment(payment)).willReturn(participants);

            SettlementResponse response = settlementService.calculate(10L);

            TransferInfo minwookTransfer = response.getTransfers().get(0);
            assertThat(minwookTransfer.getFromUserId()).isEqualTo(minwook.getId());
            assertThat(minwookTransfer.getFromUserName()).isEqualTo("민욱");
            assertThat(minwookTransfer.getToUserId()).isEqualTo(gyounghwan.getId());
            assertThat(minwookTransfer.getToUserName()).isEqualTo("경환");
            assertThat(minwookTransfer.getPaymentId()).isEqualTo(100L);
            assertThat(minwookTransfer.getPaymentTitle()).isEqualTo("장보기");
            assertThat(minwookTransfer.getParticipantId()).isEqualTo(2L);
            assertThat(minwookTransfer.isSettled()).isFalse();
        }
    }

    private User createUserWithId(Long id, String name) {
        User user = User.builder()
                .loginId("user" + id)
                .name(name)
                .build();
        return setId(User.class, user, id);
    }

    private PayRoom createPayRoomWithId(Long id, PayRoom template) {
        return setId(PayRoom.class, template, id);
    }

    private Payment createPayment(Long id, User payer, String title, int amount) {
        Payment payment = Payment.builder()
                .payRoom(payRoom)
                .payer(payer)
                .title(title)
                .amount(amount)
                .build();
        return setId(Payment.class, payment, id);
    }

    private PaymentParticipant createParticipant(Long id, Payment payment, User user) {
        PaymentParticipant pp = PaymentParticipant.builder()
                .payment(payment)
                .user(user)
                .build();
        return setId(PaymentParticipant.class, pp, id);
    }

    private <T> T setId(Class<T> clazz, T entity, Long id) {
        try {
            java.lang.reflect.Field idField = clazz.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(entity, id);
            return entity;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}