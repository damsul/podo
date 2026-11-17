package com.podo.domain.payroom.service;

import com.podo.domain.payroom.dto.request.PaymentCreateRequest;
import com.podo.domain.payroom.dto.request.PaymentUpdateRequest;
import com.podo.domain.payroom.dto.response.PaymentResponse;
import com.podo.domain.payroom.entity.PayRoom;
import com.podo.domain.payroom.entity.Payment;
import com.podo.domain.payroom.entity.PaymentParticipant;
import com.podo.domain.payroom.entity.RoomMember;
import com.podo.domain.payroom.exception.*;
import com.podo.domain.payroom.repository.PayRoomRepository;
import com.podo.domain.payroom.repository.PaymentParticipantRepository;
import com.podo.domain.payroom.repository.PaymentRepository;
import com.podo.domain.payroom.repository.RoomMemberRepository;
import com.podo.domain.user.entity.User;
import com.podo.domain.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * PaymentService 통합 테스트 (H2 + 실제 JPA)
 * - 저장·수정·삭제·정산 체크가 실제 DB에 어떻게 반영되는지 확인한다.
 * - em.flush() + em.clear() 후 다시 조회해서 "메모리 값"이 아니라 "DB 값"을 검증한다.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PaymentServiceIntegrationTest {

    @Autowired private PaymentService paymentService;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private PaymentParticipantRepository participantRepository;
    @Autowired private PayRoomRepository payRoomRepository;
    @Autowired private RoomMemberRepository roomMemberRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private EntityManager em;

    private User owner;
    private User member1;
    private User member2;
    private User outsider;
    private PayRoom payRoom;
    private PayRoom otherRoom;   // 같은 방장이 만든 다른 방

    @BeforeEach
    void setUp() {
        owner = userRepository.save(User.builder().loginId("pien").name("최경환").build());
        member1 = userRepository.save(User.builder().loginId("minwook").name("이민욱").build());
        member2 = userRepository.save(User.builder().loginId("orange").name("박주황").build());
        outsider = userRepository.save(User.builder().loginId("outsider").name("외부인").build());

        payRoom = payRoomRepository.save(PayRoom.builder().owner(owner).title("Podo MT").build());
        addMembers(payRoom, owner, member1, member2);

        otherRoom = payRoomRepository.save(PayRoom.builder().owner(owner).title("다른 방").build());
        addMembers(otherRoom, owner, member1);
    }

    @Nested
    @DisplayName("결제 추가·수정·삭제")
    class Crud {

        @Test
        @DisplayName("결제를 추가하면 결제와 참여자가 모두 저장되고, 참여자는 미정산으로 시작한다")
        void create_savesPaymentAndParticipants() {
            PaymentResponse response = createPayment(payRoom, owner, 80000, owner, member1, member2);

            flushAndClear();
            Payment saved = paymentRepository.findById(response.getId()).orElseThrow();
            List<PaymentParticipant> participants = participantRepository.findByPayment(saved);

            assertThat(saved.getTitle()).isEqualTo("장보기");
            assertThat(saved.getAmount()).isEqualTo(80000);
            assertThat(participants).hasSize(3)
                    .allMatch(pp -> !pp.isSettled())
                    .extracting(pp -> pp.getUser().getName())
                    .containsExactlyInAnyOrder("최경환", "이민욱", "박주황");
        }

        @Test
        @DisplayName("외부인을 참여자로 넣으면 NotRoomMemberException")
        void create_withOutsider_throwsException() {
            assertThatThrownBy(() -> createPayment(payRoom, owner, 80000, owner, outsider))
                    .isInstanceOf(NotRoomMemberException.class);
        }

        @Test
        @DisplayName("수정하면 기존 참여자가 모두 새 참여자로 바뀐다")
        void update_replacesParticipants() {
            PaymentResponse created = createPayment(payRoom, owner, 80000, owner, member1, member2);

            paymentService.update(payRoom.getId(), created.getId(), owner.getId(),
                    PaymentUpdateRequest.builder()
                            .title("장보기 (수정)").amount(70000).paidAt(LocalDate.of(2026, 5, 2))
                            .payerId(owner.getId())
                            .participantIds(List.of(owner.getId(), member1.getId()))
                            .build());

            flushAndClear();
            Payment updated = paymentRepository.findById(created.getId()).orElseThrow();
            assertThat(updated.getTitle()).isEqualTo("장보기 (수정)");
            assertThat(updated.getAmount()).isEqualTo(70000);
            assertThat(participantRepository.findByPayment(updated))
                    .extracting(pp -> pp.getUser().getName())
                    .containsExactlyInAnyOrder("최경환", "이민욱");
        }

        @Test
        @DisplayName("다른 방의 결제 id로 수정하면 PaymentNotFoundException (방장이 같아도)")
        void update_paymentOfOtherRoom_throwsException() {
            PaymentResponse otherPayment = createPayment(otherRoom, owner, 10000, owner, member1);

            assertThatThrownBy(() -> paymentService.update(payRoom.getId(), otherPayment.getId(), owner.getId(),
                    PaymentUpdateRequest.builder()
                            .title("수정").amount(1000).payerId(owner.getId())
                            .participantIds(List.of(owner.getId()))
                            .build()))
                    .isInstanceOf(PaymentNotFoundException.class);
        }

        @Test
        @DisplayName("삭제하면 참여자도 함께 삭제된다")
        void delete_alsoDeletesParticipants() {
            PaymentResponse created = createPayment(payRoom, owner, 80000, owner, member1);

            paymentService.delete(payRoom.getId(), created.getId(), owner.getId());

            flushAndClear();
            assertThat(paymentRepository.findById(created.getId())).isEmpty();
            assertThat(participantRepository.count()).isZero();
        }

        @Test
        @DisplayName("방 멤버가 아니면 결제 목록을 볼 수 없다")
        void findByRoom_byOutsider_throwsException() {
            assertThatThrownBy(() -> paymentService.findByRoom(payRoom.getId(), outsider.getId()))
                    .isInstanceOf(NotRoomMemberException.class);
        }
    }

    @Nested
    @DisplayName("정산 체크")
    class SettleCheck {

        @Test
        @DisplayName("방장이 정산 완료·취소하면 DB에 반영된다")
        void settleAndUnsettle_byOwner() {
            Long ppId = firstOtherParticipantId(createPayment(payRoom, owner, 80000, owner, member1));

            paymentService.markAsSettled(payRoom.getId(), ppId, owner.getId());
            flushAndClear();
            assertThat(participantRepository.findById(ppId).orElseThrow().isSettled()).isTrue();

            paymentService.markAsUnsettled(payRoom.getId(), ppId, owner.getId());
            flushAndClear();
            assertThat(participantRepository.findById(ppId).orElseThrow().isSettled()).isFalse();
        }

        @Test
        @DisplayName("받는 사람(결제자)은 자기 결제의 정산 체크를 할 수 있다")
        void settle_byReceiver() {
            // member1이 결제하고 member2가 member1에게 보내야 하는 상황
            Long ppId = firstOtherParticipantId(createPayment(payRoom, member1, 20000, member1, member2));

            paymentService.markAsSettled(payRoom.getId(), ppId, member1.getId());
            flushAndClear();

            assertThat(participantRepository.findById(ppId).orElseThrow().isSettled()).isTrue();
        }

        @Test
        @DisplayName("방장도 받는 사람도 아니면 정산 체크할 수 없다 (NotSettlementManagerException)")
        void settle_bySender_throwsException() {
            // 방장이 결제, member1이 보내는 사람 → member1은 체크 불가
            Long ppId = firstOtherParticipantId(createPayment(payRoom, owner, 80000, owner, member1));

            assertThatThrownBy(() -> paymentService.markAsSettled(payRoom.getId(), ppId, member1.getId()))
                    .isInstanceOf(NotSettlementManagerException.class);
        }

        @Test
        @DisplayName("URL의 방과 다른 방의 참여자 id면 ParticipantNotFoundException (방장이 같아도)")
        void settle_participantOfOtherRoom_throwsException() {
            Long otherPpId = firstOtherParticipantId(createPayment(otherRoom, owner, 10000, owner, member1));

            assertThatThrownBy(() -> paymentService.markAsSettled(payRoom.getId(), otherPpId, owner.getId()))
                    .isInstanceOf(ParticipantNotFoundException.class);
            assertThat(participantRepository.findById(otherPpId).orElseThrow().isSettled()).isFalse();
        }

        @Test
        @DisplayName("묶음 정산에 다른 방 참여자가 섞이면 아무것도 바뀌지 않는다")
        void settleGroup_mixedRoom_changesNothing() {
            Long myPpId = firstOtherParticipantId(createPayment(payRoom, owner, 80000, owner, member1));
            Long otherPpId = firstOtherParticipantId(createPayment(otherRoom, owner, 10000, owner, member1));

            assertThatThrownBy(() -> paymentService.markGroupAsSettled(
                    payRoom.getId(), List.of(myPpId, otherPpId), owner.getId()))
                    .isInstanceOf(ParticipantNotFoundException.class);

            assertThat(participantRepository.findById(myPpId).orElseThrow().isSettled()).isFalse();
            assertThat(participantRepository.findById(otherPpId).orElseThrow().isSettled()).isFalse();
        }
    }

    // === 테스트 헬퍼 === //

    private void addMembers(PayRoom room, User... users) {
        for (User u : users) {
            roomMemberRepository.save(RoomMember.builder().payRoom(room).user(u).build());
        }
    }

    private PaymentResponse createPayment(PayRoom room, User payer, int amount, User... participants) {
        return paymentService.create(room.getId(), payer.getId(), PaymentCreateRequest.builder()
                .title("장보기").amount(amount).paidAt(LocalDate.of(2026, 5, 1))
                .payerId(payer.getId())
                .participantIds(java.util.Arrays.stream(participants).map(User::getId).toList())
                .build());
    }

    /** 결제자가 아닌 첫 번째 참여자의 id (정산 체크 대상) */
    private Long firstOtherParticipantId(PaymentResponse response) {
        Payment payment = paymentRepository.findById(response.getId()).orElseThrow();
        Long payerId = payment.getPayer().getId();
        return participantRepository.findByPayment(payment).stream()
                .filter(pp -> !pp.getUser().getId().equals(payerId))
                .findFirst()
                .orElseThrow()
                .getId();
    }

    private void flushAndClear() {
        em.flush();
        em.clear();
    }
}