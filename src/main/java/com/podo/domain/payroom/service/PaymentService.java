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

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentParticipantRepository participantRepository;
    private final PayRoomRepository payRoomRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final UserRepository userRepository;

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

//        validateRoomMember(payRoom, findUserById(userId));
//        return paymentRepository.findByPayRoom(payRoom).stream()
//                .map(payment -> PaymentResponse.from(payment, participantRepository.findByPayment(payment)))
//                .toList();
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

    @Transactional
    public void markAsSettled(Long participantId, Long userId) {
        PaymentParticipant pp = participantRepository.findById(participantId)
                .orElseThrow(() -> new ParticipantNotFoundException(participantId));

        User currentUser = findUserById(userId);
        validateOwner(pp.getPayment().getPayRoom(), currentUser);

        pp.markAsSettled();
    }

    @Transactional
    public void markAsUnsettled(Long participantId, Long userId) {
        PaymentParticipant pp = participantRepository.findById(participantId)
                .orElseThrow(() -> new ParticipantNotFoundException(participantId));

        User currentUser = findUserById(userId);
        validateOwner(pp.getPayment().getPayRoom(), currentUser);

        pp.markAsUnsettled();
    }

    @Transactional
    public void markGroupAsSettled(List<Long> participantIds, Long userId) {
        User currentUser = findUserById(userId);
        for (Long id : participantIds) {
            PaymentParticipant pp = participantRepository.findById(id)
                    .orElseThrow(() -> new ParticipantNotFoundException(id));
            validateOwner(pp.getPayment().getPayRoom(), currentUser);
            pp.markAsSettled();
        }
    }

    @Transactional
    public void markGroupAsUnsettled(List<Long> participantIds, Long userId) {
        User currentUser = findUserById(userId);
        for (Long id : participantIds) {
            PaymentParticipant pp = participantRepository.findById(id)
                    .orElseThrow(() -> new ParticipantNotFoundException(id));
            validateOwner(pp.getPayment().getPayRoom(), currentUser);
            pp.markAsUnsettled();
        }
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

    private void validateOwner(PayRoom payRoom, User user) {
        if (!payRoom.isOwner(user)) {
            throw new NotRoomOwnerException();
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
