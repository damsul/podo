package com.podo.domain.payroom.service;

import com.podo.domain.payroom.dto.request.PaymentCreateRequest;
import com.podo.domain.payroom.dto.response.PaymentResponse;
import com.podo.domain.payroom.dto.request.PaymentUpdateRequest;
import com.podo.domain.payroom.entity.PayRoom;
import com.podo.domain.payroom.entity.Payment;
import com.podo.domain.payroom.entity.PaymentParticipant;
import com.podo.domain.payroom.entity.RoomMember;
import com.podo.domain.payroom.exception.NotRoomMemberException;
import com.podo.domain.payroom.repository.PayRoomRepository;
import com.podo.domain.payroom.repository.PaymentParticipantRepository;
import com.podo.domain.payroom.repository.PaymentRepository;
import com.podo.domain.payroom.repository.RoomMemberRepository;
import com.podo.domain.user.entity.User;
import com.podo.domain.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class PaymentServiceIntegrationTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentParticipantRepository participantRepository;

    @Autowired
    private PayRoomRepository payRoomRepository;

    @Autowired
    private RoomMemberRepository roomMemberRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager em;

    private User owner;
    private User member1;
    private User member2;
    private User nonMember;
    private PayRoom payRoom;

    @BeforeEach
    void setUp() {
        owner = userRepository.save(User.builder()
                .loginId("pien").name("최경환").build());
        member1 = userRepository.save(User.builder()
                .loginId("minwook").name("이민욱").build());
        member2 = userRepository.save(User.builder()
                .loginId("orange").name("박주황").build());
        nonMember = userRepository.save(User.builder()
                .loginId("outsider").name("외부인").build());

        payRoom = payRoomRepository.save(PayRoom.builder()
                .owner(owner).title("Podo MT").build());

        roomMemberRepository.save(RoomMember.builder().payRoom(payRoom).user(owner).build());
        roomMemberRepository.save(RoomMember.builder().payRoom(payRoom).user(member1).build());
        roomMemberRepository.save(RoomMember.builder().payRoom(payRoom).user(member2).build());
    }

    @Test
    @DisplayName("결제 항목 추가 시 Payment와 참여자들이 모두 DB에 저장된다")
    void create_realDB() {
        PaymentCreateRequest request = PaymentCreateRequest.builder()
                .title("장보기")
                .amount(80000)
                .paidAt(LocalDate.of(2026, 5, 1))
                .payerId(owner.getId())
                .participantIds(List.of(owner.getId(), member1.getId(), member2.getId()))
                .build();

        PaymentResponse response = paymentService.create(payRoom.getId(), owner.getId(), request);

        Payment savedPayment = paymentRepository.findById(response.getId()).orElseThrow();
        assertThat(savedPayment.getTitle()).isEqualTo("장보기");
        assertThat(savedPayment.getAmount()).isEqualTo(80000);

        List<PaymentParticipant> participants =
                participantRepository.findByPayment(savedPayment);
        assertThat(participants).hasSize(3);
        assertThat(participants).extracting(pp -> pp.getUser().getName())
                .containsExactlyInAnyOrder("최경환", "이민욱", "박주황");

        assertThat(participants).allMatch(pp -> !pp.isSettled());
    }

    @Test
    @DisplayName("외부인을 참여자로 추가하면 예외 발생 + DB에 아무것도 저장되지 않는다 (트랜잭션 롤백)")
    void create_withNonMember_rollback() {
        PaymentCreateRequest request = PaymentCreateRequest.builder()
                .title("장보기")
                .amount(80000)
                .payerId(owner.getId())
                .participantIds(List.of(owner.getId(), member1.getId(), nonMember.getId()))
                .build();

        assertThatThrownBy(() -> paymentService.create(payRoom.getId(), owner.getId(), request))
                .isInstanceOf(NotRoomMemberException.class);
    }

    @Test
    @DisplayName("결제 항목 수정 시 기존 참여자가 모두 새 참여자로 교체된다")
    void update_replacesParticipants() {
        PaymentResponse created = paymentService.create(payRoom.getId(), owner.getId(),
                PaymentCreateRequest.builder()
                        .title("장보기")
                        .amount(80000)
                        .payerId(owner.getId())
                        .participantIds(List.of(owner.getId(), member1.getId(), member2.getId()))
                        .build());

        paymentService.update(created.getId(), owner.getId(),
                PaymentUpdateRequest.builder()
                        .title("장보기 (수정)")
                        .amount(70000)
                        .payerId(owner.getId())
                        .participantIds(List.of(owner.getId(), member1.getId()))
                        .build());

        Payment updated = paymentRepository.findById(created.getId()).orElseThrow();
        assertThat(updated.getTitle()).isEqualTo("장보기 (수정)");
        assertThat(updated.getAmount()).isEqualTo(70000);

        List<PaymentParticipant> participants =
                participantRepository.findByPayment(updated);
        assertThat(participants).hasSize(2);
        assertThat(participants).extracting(pp -> pp.getUser().getName())
                .containsExactlyInAnyOrder("최경환", "이민욱");
    }

    @Test
    @DisplayName("결제 항목 삭제 시 참여자도 모두 함께 삭제된다")
    void delete_cascade() {
        PaymentResponse created = paymentService.create(payRoom.getId(), owner.getId(),
                PaymentCreateRequest.builder()
                        .title("장보기").amount(80000).payerId(owner.getId())
                        .participantIds(List.of(owner.getId(), member1.getId()))
                        .build());

        paymentService.delete(created.getId(), owner.getId());

        assertThat(paymentRepository.findById(created.getId())).isEmpty();

        long participantCount = participantRepository.count();
        assertThat(participantCount).isZero();
    }

    @Test
    @DisplayName("정산 토글이 DB에 반영된다")
    void settlementToggle_realDB() {
        PaymentResponse created = paymentService.create(payRoom.getId(), owner.getId(),
                PaymentCreateRequest.builder()
                        .title("장보기").amount(80000).payerId(owner.getId())
                        .participantIds(List.of(owner.getId(), member1.getId()))
                        .build());

        Payment savedPayment = paymentRepository.findById(created.getId()).orElseThrow();
        PaymentParticipant pp = participantRepository.findByPayment(savedPayment).get(0);

        paymentService.markAsSettled(pp.getId(), owner.getId());

        PaymentParticipant reloaded = participantRepository.findById(pp.getId()).orElseThrow();
        assertThat(reloaded.isSettled()).isTrue();

        paymentService.markAsUnsettled(pp.getId(), owner.getId());

        PaymentParticipant reloaded2 = participantRepository.findById(pp.getId()).orElseThrow();
        assertThat(reloaded2.isSettled()).isFalse();
    }
}
