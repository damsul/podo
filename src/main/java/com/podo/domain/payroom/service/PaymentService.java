package com.podo.domain.payroom.service;

import com.podo.domain.payroom.dto.request.PaymentCreateRequest;
import com.podo.domain.payroom.dto.response.PaymentResponse;
import com.podo.domain.payroom.dto.request.PaymentUpdateRequest;
import com.podo.domain.payroom.entity.PayRoom;
import com.podo.domain.payroom.entity.Payment;
import com.podo.domain.payroom.entity.PaymentParticipant;
import com.podo.domain.payroom.exception.*;
import com.podo.domain.payroom.repository.PayRoomRepository;
import com.podo.domain.payroom.repository.PaymentParticipantRepository;
import com.podo.domain.payroom.repository.PaymentRepository;
import com.podo.domain.payroom.repository.RoomMemberRepository;
import com.podo.domain.user.entity.User;
import com.podo.domain.user.exception.UserNotFoundException;
import com.podo.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentParticipantRepository participantRepository;
    private final PayRoomRepository payRoomRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final UserRepository userRepository;
    private final SettlementService settlementService;   // 묶음 정산 권한 확인용 (화면의 최종 정산 줄과 같은 계산)

    @Transactional
    public PaymentResponse create(Long roomId, Long userId, PaymentCreateRequest request) {
        PayRoom payRoom = findPayRoomById(roomId);
        User currentUser = findUserById(userId);

        validateRoomMember(payRoom, currentUser); // 로그인 사용자 방 멤버 검증

        User payer = findUserById(request.getPayerId());
        validateRoomMember(payRoom, payer); // 결제자 방 멤버 검증
        validatePayerSelection(payRoom, currentUser, payer);

        Payment payment = paymentRepository.save(Payment.builder()
                .payRoom(payRoom)
                .payer(payer)
                .title(request.getTitle())
                .amount(request.getAmount())
                .paidAt(request.getPaidAt())
                .build());

        List<PaymentParticipant> participants = createParticipants(payment, payRoom, request.getParticipantIds());

        return PaymentResponse.from(payment, participants);
    }

    // 쿼리: 방 1 + 멤버 확인 1 + 결제(결제자 포함) 1 + 참여자(사용자 포함) 1 → 결제 수와 무관
    public List<PaymentResponse> findByRoom(Long roomId, Long userId) {
        PayRoom payRoom = findPayRoomById(roomId);
        if (!roomMemberRepository.existsMember(roomId, userId)) {
            throw new NotRoomMemberException();
        }

        List<Payment> payments = paymentRepository.findAllByPayRoomWithPayer(payRoom);
        Map<Long, List<PaymentParticipant>> participantsByPayment = participantRepository.findGroupedByPayment(payments);

        return payments.stream()
                .map(p -> PaymentResponse.from(p, participantsByPayment.getOrDefault(p.getId(), List.of())))
                .toList();
    }

    public PaymentResponse findById(Long roomId, Long paymentId, Long userId) {
        Payment payment = findPaymentInRoom(roomId, paymentId);
        validateRoomMember(payment.getPayRoom(), findUserById(userId));
        return PaymentResponse.from(payment, participantRepository.findByPayment(payment));
    }

    @Transactional
    public PaymentResponse update(Long roomId, Long paymentId, Long userId, PaymentUpdateRequest request) {
        Payment payment = findPaymentInRoom(roomId, paymentId);
        User currentUser = findUserById(userId);

        validateModifier(payment, currentUser);

        PayRoom payRoom = payment.getPayRoom();
        User payer = findUserById(request.getPayerId());

        validateRoomMember(payRoom, payer);
        validatePayerSelection(payRoom, currentUser, payer);

        payment.update(request.getTitle(), request.getAmount(), request.getPaidAt());

        List<PaymentParticipant> oldParticipants = participantRepository.findByPayment(payment);
        participantRepository.deleteAllInBatch(oldParticipants);

        List<PaymentParticipant> participants = createParticipants(payment, payRoom, request.getParticipantIds());

        return PaymentResponse.from(payment, participants);
    }

    @Transactional
    public void delete(Long roomId, Long paymentId, Long userId) {
        Payment payment = findPaymentInRoom(roomId, paymentId);
        User user = findUserById(userId);

        validateModifier(payment, user);

        List<PaymentParticipant> participants = participantRepository.findByPayment(payment);
        participantRepository.deleteAll(participants);

        paymentRepository.delete(payment);
    }

    // ===== 정산 체크 (방장 또는 받는 사람(결제자), URL의 방에 속한 참여자만) =====

    @Transactional
    public void markAsSettled(Long roomId, Long participantId, Long userId) {
        findParticipantsForSettler(roomId, List.of(participantId), userId)
                .forEach(PaymentParticipant::markAsSettled);
    }

    @Transactional
    public void markAsUnsettled(Long roomId, Long participantId, Long userId) {
        findParticipantsForSettler(roomId, List.of(participantId), userId)
                .forEach(PaymentParticipant::markAsUnsettled);
    }

    // 묶음 정산 = 화면 "최종 정산"의 한 줄 (두 사람 사이 상계된 줄, 양방향 참여자 포함)
    @Transactional
    public void markGroupAsSettled(Long roomId, List<Long> participantIds, Long userId) {
        findParticipantsForNetLine(roomId, participantIds, userId)
                .forEach(PaymentParticipant::markAsSettled);
    }

    @Transactional
    public void markGroupAsUnsettled(Long roomId, List<Long> participantIds, Long userId) {
        findParticipantsForNetLine(roomId, participantIds, userId)
                .forEach(PaymentParticipant::markAsUnsettled);
    }

    // 1) 참여자를 전부 찾아서 URL의 방 참여자인지 확인 (아니면 404)
    // 2) 방장이면 통과
    // 3) 방장이 아니면: 요청한 묶음이 "최종 정산의 한 줄과 정확히 같고" 그 줄을 체크할 수 있어야 통과 (아니면 403)
    //    - 상계된 줄에는 내가 보낸 쪽 참여자도 섞여 있으므로, 참여자마다 결제자를 보는 방식으로는 판단할 수 없다
    //    - 화면과 같은 계산(SettlementService)을 다시 돌려 "최종 받는 사람"인지 확인한다
    //    - 묶음 일부만 골라 보내는 요청도 줄과 일치하지 않으므로 거부된다
    private List<PaymentParticipant> findParticipantsForNetLine(Long roomId, List<Long> participantIds, Long userId) {
        PayRoom payRoom = findPayRoomById(roomId);
        User currentUser = findUserById(userId);

        List<PaymentParticipant> participants = participantIds.stream()
                .map(id -> findParticipantInRoom(roomId, id))
                .toList();

        if (payRoom.isOwner(currentUser)) {
            return participants;
        }

        Set<Long> requested = new HashSet<>(participantIds);
        boolean allowed = settlementService.calculate(roomId, userId).getNetTransfers().stream()
                .anyMatch(net -> net.isCanSettle() && new HashSet<>(net.getParticipantIds()).equals(requested));
        if (!allowed) {
            throw new NotSettlementManagerException();
        }
        return participants;
    }

    // 개별 정산(결제별 한 건): 방장 또는 그 결제의 결제자(받는 사람)만 (아니면 403)
    // 하나라도 걸리면 아무것도 바꾸지 않는다 (일부만 바뀌는 상황 방지)
    private List<PaymentParticipant> findParticipantsForSettler(Long roomId, List<Long> participantIds, Long userId) {
        PayRoom payRoom = findPayRoomById(roomId);
        User currentUser = findUserById(userId);

        List<PaymentParticipant> participants = participantIds.stream()
                .map(id -> findParticipantInRoom(roomId, id))
                .toList();

        if (!payRoom.isOwner(currentUser)) {
            boolean allReceivedByMe = participants.stream()
                    .allMatch(pp -> pp.getPayment().isPaidBy(currentUser));
            if (!allReceivedByMe) {
                throw new NotSettlementManagerException();
            }
        }
        return participants;
    }

    // 다른 방의 참여자 id를 넣으면 "없는 참여자"로 처리 (findPaymentInRoom과 같은 방식, 404)
    private PaymentParticipant findParticipantInRoom(Long roomId, Long participantId) {
        PaymentParticipant pp = participantRepository.findById(participantId)
                .orElseThrow(() -> new ParticipantNotFoundException(participantId));
        if (!pp.getPayment().getPayRoom().getId().equals(roomId)) {
            throw new ParticipantNotFoundException(participantId);
        }
        return pp;
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    private PayRoom findPayRoomById(Long roomId) {
        return payRoomRepository.findById(roomId)
                .orElseThrow(() -> new PayRoomNotFoundException(roomId));
    }

    private Payment findPaymentById(Long paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
    }

    private void validateRoomMember(PayRoom payRoom, User user) {
        if (!roomMemberRepository.existsByPayRoomAndUser(payRoom, user)) {
            throw new NotRoomMemberException();
        }
    }

    private void validatePayerSelection(PayRoom payRoom, User currentUser, User payer) {
        if (!payRoom.isOwner(currentUser) && !currentUser.getId().equals(payer.getId())) {
            throw new NotPayerSelfException();
        }
    }

    private void validateModifier(Payment payment, User user) {
        PayRoom payRoom = payment.getPayRoom();
        boolean isOwner = payRoom.isOwner(user);
        boolean isPayer = payment.isPaidBy(user);

        if (!isOwner && !isPayer) {
            throw new NotPaymentModifierException();
        }
    }

    private List<PaymentParticipant> createParticipants(Payment payment, PayRoom payRoom, List<Long> participantIds) {
        return participantIds.stream()
                .map(userId -> {
                    User user = findUserById(userId);
                    validateRoomMember(payRoom, user);
                    PaymentParticipant pp = PaymentParticipant.builder()
                            .payment(payment)
                            .user(user)
                            .build();
                    return participantRepository.save(pp);
                })
                .toList();
    }

    private Payment findPaymentInRoom(Long roomId, Long paymentId) {
        Payment payment = findPaymentById(paymentId);
        if (!payment.getPayRoom().getId().equals(roomId)) {
            throw new PaymentNotFoundException(paymentId);
        }
        return payment;
    }
}