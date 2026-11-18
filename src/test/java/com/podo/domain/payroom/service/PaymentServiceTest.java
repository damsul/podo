package com.podo.domain.payroom.service;

import com.podo.domain.payroom.dto.request.PaymentCreateRequest;
import com.podo.domain.payroom.dto.request.PaymentUpdateRequest;
import com.podo.domain.payroom.dto.response.PaymentResponse;
import com.podo.domain.payroom.dto.response.SettlementResponse;
import com.podo.domain.payroom.dto.response.SettlementResponse.NetTransferInfo;
import com.podo.domain.payroom.entity.PayRoom;
import com.podo.domain.payroom.entity.Payment;
import com.podo.domain.payroom.entity.PaymentParticipant;
import com.podo.domain.payroom.exception.*;
import com.podo.domain.payroom.repository.PayRoomRepository;
import com.podo.domain.payroom.repository.PaymentParticipantRepository;
import com.podo.domain.payroom.repository.PaymentRepository;
import com.podo.domain.payroom.repository.RoomMemberRepository;
import com.podo.domain.user.entity.User;
import com.podo.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.*;

/**
 * PaymentService 단위 테스트 (Mock)
 * - 계산·저장 흐름은 통합 테스트에서 보고, 여기서는 "누가 무엇을 할 수 있는지" 권한·예외 분기만 본다.
 *
 * 등장인물: 방장(1), 멤버(2, 결제자), 멤버2(3), 외부인(99)
 * 방: Podo MT(10), 같은 방장의 다른 방(20)
 */
@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private PaymentParticipantRepository participantRepository;
    @Mock private PayRoomRepository payRoomRepository;
    @Mock private RoomMemberRepository roomMemberRepository;
    @Mock private UserRepository userRepository;
    @Mock private SettlementService settlementService;

    @InjectMocks
    private PaymentService paymentService;

    private User owner;
    private User member;
    private User member2;
    private User outsider;
    private PayRoom payRoom;
    private PayRoom otherRoom;
    private Payment payment;        // 방 10, 결제자 = member
    private Payment otherPayment;   // 방 20

    @BeforeEach
    void setUp() {
        owner = setId(User.class, User.builder().loginId("pien").name("최경환").build(), 1L);
        member = setId(User.class, User.builder().loginId("minwook").name("이민욱").build(), 2L);
        member2 = setId(User.class, User.builder().loginId("orange").name("박주황").build(), 3L);
        outsider = setId(User.class, User.builder().loginId("outsider").name("외부인").build(), 99L);

        payRoom = setId(PayRoom.class, PayRoom.builder().owner(owner).title("Podo MT").build(), 10L);
        otherRoom = setId(PayRoom.class, PayRoom.builder().owner(owner).title("다른 방").build(), 20L);

        payment = setId(Payment.class, Payment.builder()
                .payRoom(payRoom).payer(member).title("장보기").amount(80000)
                .paidAt(LocalDate.of(2026, 5, 1)).build(), 100L);
        otherPayment = setId(Payment.class, Payment.builder()
                .payRoom(otherRoom).payer(owner).title("다른 방 결제").amount(10000).build(), 101L);
    }

    @Nested
    @DisplayName("결제 추가")
    class Create {

        @Test
        @DisplayName("방장은 다른 멤버를 결제자로 지정해 결제를 추가할 수 있다")
        void create_byOwner_success() {
            PaymentCreateRequest request = PaymentCreateRequest.builder()
                    .title("장보기").amount(80000).paidAt(LocalDate.of(2026, 5, 1))
                    .payerId(2L).participantIds(List.of(1L, 2L))
                    .build();

            given(payRoomRepository.findById(10L)).willReturn(Optional.of(payRoom));
            given(userRepository.findById(1L)).willReturn(Optional.of(owner));
            given(userRepository.findById(2L)).willReturn(Optional.of(member));
            given(roomMemberRepository.existsByPayRoomAndUser(payRoom, owner)).willReturn(true);
            given(roomMemberRepository.existsByPayRoomAndUser(payRoom, member)).willReturn(true);
            given(paymentRepository.save(any(Payment.class))).willAnswer(inv -> inv.getArgument(0));
            given(participantRepository.save(any(PaymentParticipant.class))).willAnswer(inv -> inv.getArgument(0));

            PaymentResponse response = paymentService.create(10L, 1L, request);

            assertThat(response.getTitle()).isEqualTo("장보기");
            assertThat(response.getAmount()).isEqualTo(80000);
            assertThat(response.getParticipants()).hasSize(2);
            verify(participantRepository, times(2)).save(any(PaymentParticipant.class));
        }

        @Test
        @DisplayName("방 멤버가 아니면 결제를 추가할 수 없다")
        void create_byOutsider_throwsException() {
            PaymentCreateRequest request = PaymentCreateRequest.builder()
                    .title("장보기").amount(80000).payerId(99L).participantIds(List.of(99L))
                    .build();

            given(payRoomRepository.findById(10L)).willReturn(Optional.of(payRoom));
            given(userRepository.findById(99L)).willReturn(Optional.of(outsider));
            given(roomMemberRepository.existsByPayRoomAndUser(payRoom, outsider)).willReturn(false);

            assertThatThrownBy(() -> paymentService.create(10L, 99L, request))
                    .isInstanceOf(NotRoomMemberException.class);

            verify(paymentRepository, never()).save(any(Payment.class));
        }

        @Test
        @DisplayName("일반 멤버가 다른 사람을 결제자로 지정하면 NotPayerSelfException")
        void create_memberPicksOtherPayer_throwsException() {
            PaymentCreateRequest request = PaymentCreateRequest.builder()
                    .title("장보기").amount(80000).payerId(1L).participantIds(List.of(1L, 2L))
                    .build();

            given(payRoomRepository.findById(10L)).willReturn(Optional.of(payRoom));
            given(userRepository.findById(2L)).willReturn(Optional.of(member));
            given(userRepository.findById(1L)).willReturn(Optional.of(owner));
            given(roomMemberRepository.existsByPayRoomAndUser(payRoom, member)).willReturn(true);
            given(roomMemberRepository.existsByPayRoomAndUser(payRoom, owner)).willReturn(true);

            assertThatThrownBy(() -> paymentService.create(10L, 2L, request))
                    .isInstanceOf(NotPayerSelfException.class);

            verify(paymentRepository, never()).save(any(Payment.class));
        }

        @Test
        @DisplayName("참여자 중 외부인이 있으면 NotRoomMemberException")
        void create_participantIsOutsider_throwsException() {
            PaymentCreateRequest request = PaymentCreateRequest.builder()
                    .title("장보기").amount(80000).payerId(1L).participantIds(List.of(1L, 99L))
                    .build();

            given(payRoomRepository.findById(10L)).willReturn(Optional.of(payRoom));
            given(userRepository.findById(1L)).willReturn(Optional.of(owner));
            given(userRepository.findById(99L)).willReturn(Optional.of(outsider));
            given(roomMemberRepository.existsByPayRoomAndUser(payRoom, owner)).willReturn(true);
            given(roomMemberRepository.existsByPayRoomAndUser(payRoom, outsider)).willReturn(false);
            given(paymentRepository.save(any(Payment.class))).willAnswer(inv -> inv.getArgument(0));
            given(participantRepository.save(any(PaymentParticipant.class))).willAnswer(inv -> inv.getArgument(0));

            assertThatThrownBy(() -> paymentService.create(10L, 1L, request))
                    .isInstanceOf(NotRoomMemberException.class);
        }
    }

    @Nested
    @DisplayName("결제 조회")
    class Find {

        @Test
        @DisplayName("방 결제 목록은 결제 수와 상관없이 묶어서 조회한다")
        void findByRoom_success() {
            PaymentParticipant pp = participant(500L, payment, member);

            given(payRoomRepository.findById(10L)).willReturn(Optional.of(payRoom));
            given(roomMemberRepository.existsMember(10L, 1L)).willReturn(true);
            given(paymentRepository.findAllByPayRoomWithPayer(payRoom)).willReturn(List.of(payment));
            given(participantRepository.findGroupedByPayment(anyList())).willReturn(Map.of(100L, List.of(pp)));

            List<PaymentResponse> responses = paymentService.findByRoom(10L, 1L);

            assertThat(responses).hasSize(1);
            assertThat(responses.get(0).getParticipants()).hasSize(1);
        }

        @Test
        @DisplayName("방 멤버가 아니면 결제 목록을 볼 수 없다")
        void findByRoom_byOutsider_throwsException() {
            given(payRoomRepository.findById(10L)).willReturn(Optional.of(payRoom));
            given(roomMemberRepository.existsMember(10L, 99L)).willReturn(false);

            assertThatThrownBy(() -> paymentService.findByRoom(10L, 99L))
                    .isInstanceOf(NotRoomMemberException.class);

            verify(paymentRepository, never()).findAllByPayRoomWithPayer(any());
        }

        @Test
        @DisplayName("URL의 방과 결제의 방이 다르면 PaymentNotFoundException (404)")
        void findById_otherRoom_throwsException() {
            given(paymentRepository.findById(100L)).willReturn(Optional.of(payment));

            assertThatThrownBy(() -> paymentService.findById(20L, 100L, 1L))
                    .isInstanceOf(PaymentNotFoundException.class);
        }

        @Test
        @DisplayName("방 멤버가 아니면 결제 상세를 볼 수 없다")
        void findById_byOutsider_throwsException() {
            given(paymentRepository.findById(100L)).willReturn(Optional.of(payment));
            given(userRepository.findById(99L)).willReturn(Optional.of(outsider));
            given(roomMemberRepository.existsByPayRoomAndUser(payRoom, outsider)).willReturn(false);

            assertThatThrownBy(() -> paymentService.findById(10L, 100L, 99L))
                    .isInstanceOf(NotRoomMemberException.class);
        }
    }

    @Nested
    @DisplayName("결제 수정·삭제")
    class UpdateAndDelete {

        @Test
        @DisplayName("결제자 본인은 수정할 수 있고, 기존 참여자는 지우고 새로 만든다")
        void update_byPayer_success() {
            PaymentParticipant old = participant(500L, payment, member);
            PaymentUpdateRequest request = PaymentUpdateRequest.builder()
                    .title("장보기 (수정)").amount(90000).paidAt(LocalDate.of(2026, 5, 2))
                    .payerId(2L).participantIds(List.of(2L))
                    .build();

            given(paymentRepository.findById(100L)).willReturn(Optional.of(payment));
            given(userRepository.findById(2L)).willReturn(Optional.of(member));
            given(roomMemberRepository.existsByPayRoomAndUser(payRoom, member)).willReturn(true);
            given(participantRepository.findByPayment(payment)).willReturn(List.of(old));
            given(participantRepository.save(any(PaymentParticipant.class))).willAnswer(inv -> inv.getArgument(0));

            PaymentResponse response = paymentService.update(10L, 100L, 2L, request);

            assertThat(response.getTitle()).isEqualTo("장보기 (수정)");
            assertThat(response.getAmount()).isEqualTo(90000);
            verify(participantRepository).deleteAllInBatch(List.of(old));
            verify(participantRepository, times(1)).save(any(PaymentParticipant.class));
        }

        @Test
        @DisplayName("방장도 결제자도 아니면 수정할 수 없다")
        void update_byOtherMember_throwsException() {
            PaymentUpdateRequest request = PaymentUpdateRequest.builder()
                    .title("몰래 수정").amount(1).payerId(3L).participantIds(List.of(3L))
                    .build();

            given(paymentRepository.findById(100L)).willReturn(Optional.of(payment));
            given(userRepository.findById(3L)).willReturn(Optional.of(member2));

            assertThatThrownBy(() -> paymentService.update(10L, 100L, 3L, request))
                    .isInstanceOf(NotPaymentModifierException.class);

            assertThat(payment.getTitle()).isEqualTo("장보기");   // 그대로
            verify(participantRepository, never()).deleteAllInBatch(anyList());
        }

        @Test
        @DisplayName("다른 방의 결제 id로 수정하면 PaymentNotFoundException")
        void update_otherRoom_throwsException() {
            PaymentUpdateRequest request = PaymentUpdateRequest.builder()
                    .title("수정").amount(1000).payerId(1L).participantIds(List.of(1L))
                    .build();

            given(paymentRepository.findById(101L)).willReturn(Optional.of(otherPayment));

            assertThatThrownBy(() -> paymentService.update(10L, 101L, 1L, request))
                    .isInstanceOf(PaymentNotFoundException.class);
        }

        @Test
        @DisplayName("방장은 다른 사람이 결제한 항목도 삭제할 수 있다 (참여자 먼저 삭제)")
        void delete_byOwner_success() {
            List<PaymentParticipant> participants = List.of(participant(500L, payment, member));

            given(paymentRepository.findById(100L)).willReturn(Optional.of(payment));
            given(userRepository.findById(1L)).willReturn(Optional.of(owner));
            given(participantRepository.findByPayment(payment)).willReturn(participants);

            paymentService.delete(10L, 100L, 1L);

            verify(participantRepository).deleteAll(participants);
            verify(paymentRepository).delete(payment);
        }

        @Test
        @DisplayName("방장도 결제자도 아니면 삭제할 수 없다")
        void delete_byOtherMember_throwsException() {
            given(paymentRepository.findById(100L)).willReturn(Optional.of(payment));
            given(userRepository.findById(3L)).willReturn(Optional.of(member2));

            assertThatThrownBy(() -> paymentService.delete(10L, 100L, 3L))
                    .isInstanceOf(NotPaymentModifierException.class);

            verify(paymentRepository, never()).delete(any(Payment.class));
        }
    }

    @Nested
    @DisplayName("정산 체크 (방장 또는 받는 사람, 그 방의 참여자만)")
    class SettleCheck {

        @Test
        @DisplayName("방장은 다른 사람이 결제한 항목도 정산 완료로 표시할 수 있다")
        void markAsSettled_byOwner_success() {
            PaymentParticipant pp = participant(500L, payment, member2);   // 결제자는 member
            given(payRoomRepository.findById(10L)).willReturn(Optional.of(payRoom));
            given(userRepository.findById(1L)).willReturn(Optional.of(owner));
            given(participantRepository.findById(500L)).willReturn(Optional.of(pp));

            paymentService.markAsSettled(10L, 500L, 1L);

            assertThat(pp.isSettled()).isTrue();
        }

        @Test
        @DisplayName("받는 사람(결제자)은 자기 결제의 정산 완료를 표시할 수 있다")
        void markAsSettled_byReceiver_success() {
            PaymentParticipant pp = participant(500L, payment, member2);   // member2 → member에게 송금
            given(payRoomRepository.findById(10L)).willReturn(Optional.of(payRoom));
            given(userRepository.findById(2L)).willReturn(Optional.of(member));
            given(participantRepository.findById(500L)).willReturn(Optional.of(pp));

            paymentService.markAsSettled(10L, 500L, 2L);

            assertThat(pp.isSettled()).isTrue();
        }

        @Test
        @DisplayName("받는 사람(결제자)은 정산 완료를 취소할 수 있다")
        void markAsUnsettled_byReceiver_success() {
            PaymentParticipant pp = participant(500L, payment, member2);
            pp.markAsSettled();
            given(payRoomRepository.findById(10L)).willReturn(Optional.of(payRoom));
            given(userRepository.findById(2L)).willReturn(Optional.of(member));
            given(participantRepository.findById(500L)).willReturn(Optional.of(pp));

            paymentService.markAsUnsettled(10L, 500L, 2L);

            assertThat(pp.isSettled()).isFalse();
        }

        @Test
        @DisplayName("방장도 받는 사람도 아니면 NotSettlementManagerException (보내는 사람 본인도 불가)")
        void markAsSettled_bySender_throwsException() {
            PaymentParticipant pp = participant(500L, payment, member2);   // member2가 보내는 사람
            given(payRoomRepository.findById(10L)).willReturn(Optional.of(payRoom));
            given(userRepository.findById(3L)).willReturn(Optional.of(member2));
            given(participantRepository.findById(500L)).willReturn(Optional.of(pp));

            assertThatThrownBy(() -> paymentService.markAsSettled(10L, 500L, 3L))
                    .isInstanceOf(NotSettlementManagerException.class);

            assertThat(pp.isSettled()).isFalse();
        }

        @Test
        @DisplayName("다른 방의 참여자 id면 ParticipantNotFoundException (두 방의 방장이 같아도 막는다)")
        void markAsSettled_participantOfOtherRoom_throwsException() {
            PaymentParticipant pp = participant(500L, payment, member);   // 방 10의 참여자
            given(payRoomRepository.findById(20L)).willReturn(Optional.of(otherRoom));
            given(userRepository.findById(1L)).willReturn(Optional.of(owner));
            given(participantRepository.findById(500L)).willReturn(Optional.of(pp));

            assertThatThrownBy(() -> paymentService.markAsSettled(20L, 500L, 1L))
                    .isInstanceOf(ParticipantNotFoundException.class);

            assertThat(pp.isSettled()).isFalse();
        }

        @Test
        @DisplayName("묶음 정산: 최종 받는 사람은 상계된 줄(양방향 참여자 포함)을 한 번에 정산할 수 있다")
        void markGroupAsSettled_byNetReceiver_success() {
            // member2 → member (장보기), member → member2 (택시)가 상계된 한 줄, 최종 받는 사람 = member
            Payment taxi = setId(Payment.class, Payment.builder()
                    .payRoom(payRoom).payer(member2).title("택시").amount(8000).build(), 102L);
            PaymentParticipant pp1 = participant(500L, payment, member2);   // member2가 member에게
            PaymentParticipant pp2 = participant(501L, taxi, member);       // member가 member2에게
            given(payRoomRepository.findById(10L)).willReturn(Optional.of(payRoom));
            given(userRepository.findById(2L)).willReturn(Optional.of(member));
            given(participantRepository.findById(500L)).willReturn(Optional.of(pp1));
            given(participantRepository.findById(501L)).willReturn(Optional.of(pp2));
            given(settlementService.calculate(10L, 2L)).willReturn(netLines(netLine(true, 500L, 501L)));

            paymentService.markGroupAsSettled(10L, List.of(500L, 501L), 2L);

            assertThat(pp1.isSettled()).isTrue();
            assertThat(pp2.isSettled()).isTrue();
        }

        @Test
        @DisplayName("묶음 정산: 최종 보내는 사람은 그 줄을 정산할 수 없다")
        void markGroupAsSettled_byNetSender_throwsException() {
            Payment taxi = setId(Payment.class, Payment.builder()
                    .payRoom(payRoom).payer(member2).title("택시").amount(8000).build(), 102L);
            PaymentParticipant pp1 = participant(500L, payment, member2);
            PaymentParticipant pp2 = participant(501L, taxi, member);
            given(payRoomRepository.findById(10L)).willReturn(Optional.of(payRoom));
            given(userRepository.findById(3L)).willReturn(Optional.of(member2));
            given(participantRepository.findById(500L)).willReturn(Optional.of(pp1));
            given(participantRepository.findById(501L)).willReturn(Optional.of(pp2));
            given(settlementService.calculate(10L, 3L)).willReturn(netLines(netLine(false, 500L, 501L)));

            assertThatThrownBy(() -> paymentService.markGroupAsSettled(10L, List.of(500L, 501L), 3L))
                    .isInstanceOf(NotSettlementManagerException.class);

            assertThat(pp1.isSettled()).isFalse();
            assertThat(pp2.isSettled()).isFalse();
        }

        @Test
        @DisplayName("묶음 정산: 줄의 일부만 골라 보내면 거부된다 (받는 사람이라도)")
        void markGroupAsSettled_partialLine_throwsException() {
            PaymentParticipant pp1 = participant(500L, payment, member2);
            given(payRoomRepository.findById(10L)).willReturn(Optional.of(payRoom));
            given(userRepository.findById(2L)).willReturn(Optional.of(member));
            given(participantRepository.findById(500L)).willReturn(Optional.of(pp1));
            given(settlementService.calculate(10L, 2L)).willReturn(netLines(netLine(true, 500L, 501L)));

            assertThatThrownBy(() -> paymentService.markGroupAsSettled(10L, List.of(500L), 2L))
                    .isInstanceOf(NotSettlementManagerException.class);

            assertThat(pp1.isSettled()).isFalse();
        }

        @Test
        @DisplayName("묶음 정산: 방장은 정산 계산 없이 바로 처리한다")
        void markGroupAsSettled_byOwner_skipsCalculation() {
            PaymentParticipant pp1 = participant(500L, payment, member2);
            given(payRoomRepository.findById(10L)).willReturn(Optional.of(payRoom));
            given(userRepository.findById(1L)).willReturn(Optional.of(owner));
            given(participantRepository.findById(500L)).willReturn(Optional.of(pp1));

            paymentService.markGroupAsSettled(10L, List.of(500L), 1L);

            assertThat(pp1.isSettled()).isTrue();
            verify(settlementService, never()).calculate(any(), any());
        }

        @Test
        @DisplayName("묶음 중 하나라도 다른 방 참여자면 아무것도 바뀌지 않는다")
        void markGroupAsSettled_mixedRoom_changesNothing() {
            PaymentParticipant mine = participant(500L, payment, member);
            PaymentParticipant others = participant(501L, otherPayment, owner);
            given(payRoomRepository.findById(10L)).willReturn(Optional.of(payRoom));
            given(userRepository.findById(1L)).willReturn(Optional.of(owner));
            given(participantRepository.findById(500L)).willReturn(Optional.of(mine));
            given(participantRepository.findById(501L)).willReturn(Optional.of(others));

            assertThatThrownBy(() -> paymentService.markGroupAsSettled(10L, List.of(500L, 501L), 1L))
                    .isInstanceOf(ParticipantNotFoundException.class);

            assertThat(mine.isSettled()).isFalse();
            assertThat(others.isSettled()).isFalse();
        }
    }

    // === 테스트 헬퍼 === //

    private SettlementResponse netLines(NetTransferInfo... lines) {
        return SettlementResponse.builder().netTransfers(List.of(lines)).build();
    }

    private NetTransferInfo netLine(boolean canSettle, Long... participantIds) {
        return NetTransferInfo.builder().canSettle(canSettle).participantIds(List.of(participantIds)).build();
    }

    private PaymentParticipant participant(Long id, Payment payment, User user) {
        return setId(PaymentParticipant.class,
                PaymentParticipant.builder().payment(payment).user(user).build(), id);
    }

    private static <T> T setId(Class<T> clazz, T entity, Long id) {
        try {
            Field idField = clazz.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(entity, id);
            return entity;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}