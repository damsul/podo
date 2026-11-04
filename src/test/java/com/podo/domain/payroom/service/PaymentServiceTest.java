package com.podo.domain.payroom.service;

import com.podo.domain.payroom.dto.request.PaymentCreateRequest;
import com.podo.domain.payroom.dto.response.PaymentResponse;
import com.podo.domain.payroom.dto.request.PaymentUpdateRequest;
import com.podo.domain.payroom.entity.PayRoom;
import com.podo.domain.payroom.entity.Payment;
import com.podo.domain.payroom.entity.PaymentParticipant;
import com.podo.domain.payroom.exception.NotRoomMemberException;
import com.podo.domain.payroom.exception.PayRoomNotFoundException;
import com.podo.domain.payroom.exception.PaymentNotFoundException;
import com.podo.domain.payroom.repository.PayRoomRepository;
import com.podo.domain.payroom.repository.PaymentParticipantRepository;
import com.podo.domain.payroom.repository.PaymentRepository;
import com.podo.domain.payroom.repository.RoomMemberRepository;
import com.podo.domain.user.entity.User;
import com.podo.domain.user.exception.UserNotFoundException;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentParticipantRepository participantRepository;

    @Mock
    private PayRoomRepository payRoomRepository;

    @Mock
    private RoomMemberRepository roomMemberRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PaymentService paymentService;

    private User payer;
    private User member1;
    private User member2;
    private User nonMember;
    private PayRoom payRoom;
    private Payment payment;

    @BeforeEach
    void setUp() {
        payer = createUserWithId(1L, User.builder()
                .loginId("pien").name("최경환").build());

        member1 = createUserWithId(2L, User.builder()
                .loginId("minwook").name("이민욱").build());

        member2 = createUserWithId(3L, User.builder()
                .loginId("orange").name("박주황").build());

        nonMember = createUserWithId(99L, User.builder()
                .loginId("outsider").name("외부인").build());

        payRoom = createPayRoomWithId(10L, PayRoom.builder()
                .owner(payer).title("Podo MT").build());

        payment = createPaymentWithId(100L, Payment.builder()
                .payRoom(payRoom)
                .payer(payer)
                .title("장보기")
                .amount(80000)
                .paidAt(LocalDate.of(2026, 5, 1))
                .build());
    }

    @Nested
    @DisplayName("결제 항목 추가 테스트")
    class CreatePayment {

        @Test
        @DisplayName("결제 항목을 추가하면 Payment와 PaymentParticipant들이 모두 생성된다")
        void create_success() {
            PaymentCreateRequest request = PaymentCreateRequest.builder()
                    .title("장보기")
                    .amount(80000)
                    .paidAt(LocalDate.of(2026, 5, 1))
                    .payerId(1L)
                    .participantIds(List.of(1L, 2L, 3L))
                    .build();

            given(payRoomRepository.findById(10L)).willReturn(Optional.of(payRoom));
            given(userRepository.findById(1L)).willReturn(Optional.of(payer));
            given(userRepository.findById(2L)).willReturn(Optional.of(member1));
            given(userRepository.findById(3L)).willReturn(Optional.of(member2));

            given(roomMemberRepository.existsByPayRoomAndUser(payRoom, payer)).willReturn(true);
            given(roomMemberRepository.existsByPayRoomAndUser(payRoom, member1)).willReturn(true);
            given(roomMemberRepository.existsByPayRoomAndUser(payRoom, member2)).willReturn(true);

            given(paymentRepository.save(any(Payment.class))).willReturn(payment);
            given(participantRepository.save(any(PaymentParticipant.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            PaymentResponse response = paymentService.create(10L, 1L, request);

            assertThat(response.getTitle()).isEqualTo("장보기");
            assertThat(response.getAmount()).isEqualTo(80000);
            assertThat(response.getParticipants()).hasSize(3);

            verify(paymentRepository).save(any(Payment.class));
            verify(participantRepository, times(3)).save(any(PaymentParticipant.class));
        }

        @Test
        @DisplayName("결제자가 방 멤버가 아니면 NotRoomMemberException 발생")
        void create_payerNotMember() {
            PaymentCreateRequest request = PaymentCreateRequest.builder()
                    .title("장보기").amount(80000).payerId(99L)
                    .participantIds(List.of(99L))
                    .build();

            given(payRoomRepository.findById(10L)).willReturn(Optional.of(payRoom));
            given(userRepository.findById(1L)).willReturn(Optional.of(payer));
            given(roomMemberRepository.existsByPayRoomAndUser(payRoom, payer)).willReturn(true);
            given(userRepository.findById(99L)).willReturn(Optional.of(nonMember));
            given(roomMemberRepository.existsByPayRoomAndUser(payRoom, nonMember)).willReturn(false);

            assertThatThrownBy(() -> paymentService.create(10L, 1L, request))
                    .isInstanceOf(NotRoomMemberException.class);

            verify(paymentRepository, never()).save(any(Payment.class));
            verify(participantRepository, never()).save(any(PaymentParticipant.class));
        }

        @Test
        @DisplayName("참여자 중 한 명이라도 방 멤버가 아니면 예외 발생")
        void create_participantNotMember() {
            PaymentCreateRequest request = PaymentCreateRequest.builder()
                    .title("장보기").amount(80000).payerId(1L)
                    .participantIds(List.of(1L, 2L, 99L))
                    .build();

            given(payRoomRepository.findById(10L)).willReturn(Optional.of(payRoom));
            given(userRepository.findById(1L)).willReturn(Optional.of(payer));
            given(userRepository.findById(2L)).willReturn(Optional.of(member1));
            given(userRepository.findById(99L)).willReturn(Optional.of(nonMember));

            given(roomMemberRepository.existsByPayRoomAndUser(payRoom, payer)).willReturn(true);
            given(roomMemberRepository.existsByPayRoomAndUser(payRoom, member1)).willReturn(true);
            given(roomMemberRepository.existsByPayRoomAndUser(payRoom, nonMember)).willReturn(false);

            given(paymentRepository.save(any(Payment.class))).willReturn(payment);
            given(participantRepository.save(any(PaymentParticipant.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            assertThatThrownBy(() -> paymentService.create(10L, 1L, request))
                    .isInstanceOf(NotRoomMemberException.class);
        }

        @Test
        @DisplayName("존재하지 않는 방이면 PayRoomNotFoundException 발생")
        void create_roomNotFound() {
            PaymentCreateRequest request = PaymentCreateRequest.builder()
                    .title("장보기").amount(80000).payerId(1L)
                    .participantIds(List.of(1L))
                    .build();

            given(payRoomRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> paymentService.create(999L, 1L, request))
                    .isInstanceOf(PayRoomNotFoundException.class);

            verify(paymentRepository, never()).save(any(Payment.class));
        }

        @Test
        @DisplayName("존재하지 않는 사용자가 결제자면 UserNotFoundException 발생")
        void create_payerNotFound() {
            PaymentCreateRequest request = PaymentCreateRequest.builder()
                    .title("장보기").amount(80000).payerId(999L)
                    .participantIds(List.of(999L))
                    .build();

            given(payRoomRepository.findById(10L)).willReturn(Optional.of(payRoom));
            given(userRepository.findById(1L)).willReturn(Optional.of(payer));
            given(roomMemberRepository.existsByPayRoomAndUser(payRoom, payer)).willReturn(true);
            given(userRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> paymentService.create(10L, 1L, request))
                    .isInstanceOf(UserNotFoundException.class);
        }

        @Test
        @DisplayName("방 멤버가 아닌 사용자가 결제를 추가하면 NotRoomMemberException")
        void create_byNonRoomMember_throwsException() {
            PaymentCreateRequest request = PaymentCreateRequest.builder()
                    .title("장보기").amount(80000).payerId(1L)
                    .participantIds(List.of(1L))
                    .build();

            given(payRoomRepository.findById(10L)).willReturn(Optional.of(payRoom));
            given(userRepository.findById(99L)).willReturn(Optional.of(nonMember));
            given(roomMemberRepository.existsByPayRoomAndUser(payRoom, nonMember)).willReturn(false);

            assertThatThrownBy(() -> paymentService.create(10L, 99L, request))
                    .isInstanceOf(NotRoomMemberException.class);

            verify(paymentRepository, never()).save(any(Payment.class));
        }
    }


    @Nested
    @DisplayName("결제 항목 조회 테스트")
    class FindPayment {

        @Test
        @DisplayName("ID로 결제 항목을 조회한다")
        void findById_success() {
            PaymentParticipant pp1 = createParticipant(payment, payer);
            PaymentParticipant pp2 = createParticipant(payment, member1);

            given(paymentRepository.findById(100L)).willReturn(Optional.of(payment));
            given(participantRepository.findByPayment(payment))
                    .willReturn(List.of(pp1, pp2));

            PaymentResponse response = paymentService.findById(100L);

            assertThat(response.getId()).isEqualTo(100L);
            assertThat(response.getTitle()).isEqualTo("장보기");
            assertThat(response.getParticipants()).hasSize(2);
        }

        @Test
        @DisplayName("존재하지 않는 결제 항목 조회 시 PaymentNotFoundException")
        void findById_notFound() {
            given(paymentRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> paymentService.findById(999L))
                    .isInstanceOf(PaymentNotFoundException.class)
                    .hasMessageContaining("999");
        }

        @Test
        @DisplayName("방의 모든 결제 항목을 조회한다")
        void findByRoom_success() {
            Payment payment2 = createPaymentWithId(101L, Payment.builder()
                    .payRoom(payRoom).payer(payer)
                    .title("야식").amount(20000).build());

            given(payRoomRepository.findById(10L)).willReturn(Optional.of(payRoom));
            given(paymentRepository.findByPayRoom(payRoom)).willReturn(List.of(payment, payment2));
            given(participantRepository.findByPayment(payment)).willReturn(List.of(createParticipant(payment, payer)));
            given(participantRepository.findByPayment(payment2)).willReturn(List.of(createParticipant(payment2, payer)));

            List<PaymentResponse> responses = paymentService.findByRoom(10L);

            assertThat(responses).hasSize(2);
            assertThat(responses).extracting(PaymentResponse::getTitle)
                    .containsExactly("장보기", "야식");
        }
    }

    @Nested
    @DisplayName("결제 항목 수정 테스트")
    class UpdatePayment {

        @Test
        @DisplayName("결제 항목 수정 시 기존 참여자는 삭제되고 새로 생성된다")
        void update_replaceParticipants() {
            PaymentUpdateRequest request = PaymentUpdateRequest.builder()
                    .title("장보기 (수정)")
                    .amount(90000)
                    .paidAt(LocalDate.of(2026, 5, 2))
                    .payerId(1L)
                    .participantIds(List.of(1L, 2L))
                    .build();

            PaymentParticipant oldPp1 = createParticipant(payment, payer);
            PaymentParticipant oldPp2 = createParticipant(payment, member1);

            given(paymentRepository.findById(100L)).willReturn(Optional.of(payment));
            given(userRepository.findById(1L)).willReturn(Optional.of(payer));
            given(userRepository.findById(2L)).willReturn(Optional.of(member1));
            given(roomMemberRepository.existsByPayRoomAndUser(payRoom, payer)).willReturn(true);
            given(roomMemberRepository.existsByPayRoomAndUser(payRoom, member1)).willReturn(true);

            given(participantRepository.findByPayment(payment)).willReturn(List.of(oldPp1, oldPp2));

            given(participantRepository.save(any(PaymentParticipant.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            PaymentResponse response = paymentService.update(100L, 1L, request);

            assertThat(response.getTitle()).isEqualTo("장보기 (수정)");
            assertThat(response.getAmount()).isEqualTo(90000);

            verify(participantRepository).findByPayment(payment);
            verify(participantRepository).deleteAllInBatch(anyList());
            verify(participantRepository, times(2))
                    .save(any(PaymentParticipant.class));
        }
    }

    @Nested
    @DisplayName("결제 항목 삭제 테스트")
    class DeletePayment {

        @Test
        @DisplayName("결제 항목 삭제 시 참여자도 모두 삭제된다")
        void delete_alsoDeletesParticipants() {
            PaymentParticipant pp1 = createParticipant(payment, payer);
            PaymentParticipant pp2 = createParticipant(payment, member1);

            given(paymentRepository.findById(100L)).willReturn(Optional.of(payment));
            given(userRepository.findById(1L)).willReturn(Optional.of(payer));
            given(participantRepository.findByPayment(payment)).willReturn(List.of(pp1, pp2));

            paymentService.delete(100L, 1L);

            verify(participantRepository).findByPayment(payment);
            verify(participantRepository).deleteAll(anyList());
            verify(paymentRepository).delete(payment);
        }
    }

    @Nested
    @DisplayName("정산 상태 토글 테스트")
    class SettlementToggle {

        @Test
        @DisplayName("참여자를 정산 완료로 표시한다")
        void markAsSettled() {
            PaymentParticipant pp = createParticipant(payment, payer);
            given(participantRepository.findById(1L)).willReturn(Optional.of(pp));
            given(userRepository.findById(1L)).willReturn(Optional.of(payer));
            given(roomMemberRepository.existsByPayRoomAndUser(payRoom, payer)).willReturn(true);

            paymentService.markAsSettled(1L, 1L);

            assertThat(pp.isSettled()).isTrue();
        }

        @Test
        @DisplayName("정산 완료를 다시 미정산으로 되돌린다")
        void markAsUnsettled() {
            PaymentParticipant pp = createParticipant(payment, payer);
            pp.markAsSettled();
            given(participantRepository.findById(1L)).willReturn(Optional.of(pp));
            given(userRepository.findById(1L)).willReturn(Optional.of(payer));
            given(roomMemberRepository.existsByPayRoomAndUser(payRoom, payer)).willReturn(true);

            paymentService.markAsUnsettled(1L, 1L);

            assertThat(pp.isSettled()).isFalse();
        }

        @Test
        @DisplayName("방 멤버가 아닌 사용자가 정산 완료를 시도하면 NotRoomMemberException")
        void markAsSettled_byNonRoomMember_throwsException() {
            PaymentParticipant pp = createParticipant(payment, payer);
            given(participantRepository.findById(1L)).willReturn(Optional.of(pp));
            given(userRepository.findById(99L)).willReturn(Optional.of(nonMember));
            given(roomMemberRepository.existsByPayRoomAndUser(payRoom, nonMember)).willReturn(false);

            assertThatThrownBy(() -> paymentService.markAsSettled(1L, 99L))
                    .isInstanceOf(NotRoomMemberException.class);

            assertThat(pp.isSettled()).isFalse();   // 토글 안 됨 확인!
        }

        @Test
        @DisplayName("방 멤버가 아닌 사용자가 정산 취소를 시도하면 NotRoomMemberException")
        void markAsUnsettled_byNonRoomMember_throwsException() {
            PaymentParticipant pp = createParticipant(payment, payer);
            pp.markAsSettled();   // 미리 완료 상태로
            given(participantRepository.findById(1L)).willReturn(Optional.of(pp));
            given(userRepository.findById(99L)).willReturn(Optional.of(nonMember));
            given(roomMemberRepository.existsByPayRoomAndUser(payRoom, nonMember)).willReturn(false);

            assertThatThrownBy(() -> paymentService.markAsUnsettled(1L, 99L))
                    .isInstanceOf(NotRoomMemberException.class);

            assertThat(pp.isSettled()).isTrue();   // 여전히 완료 상태 (취소 안 됨)!
        }
    }

    private User createUserWithId(Long id, User template) {
        try {
            Field idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(template, id);
            return template;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private PayRoom createPayRoomWithId(Long id, PayRoom template) {
        try {
            Field idField = PayRoom.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(template, id);
            return template;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Payment createPaymentWithId(Long id, Payment template) {
        try {
            Field idField = Payment.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(template, id);
            return template;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private PaymentParticipant createParticipant(Payment payment, User user) {
        return PaymentParticipant.builder()
                .payment(payment)
                .user(user)
                .build();
    }
}