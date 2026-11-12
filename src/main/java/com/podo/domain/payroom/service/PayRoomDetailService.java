package com.podo.domain.payroom.service;

import com.podo.domain.payroom.dto.response.AccountResponse;
import com.podo.domain.payroom.dto.response.MemberInfo;
import com.podo.domain.payroom.dto.response.PayRoomDetailResponse;
import com.podo.domain.payroom.dto.response.PayRoomResponse;
import com.podo.domain.payroom.dto.response.PaymentResponse;
import com.podo.domain.payroom.entity.PayRoom;
import com.podo.domain.payroom.entity.Payment;
import com.podo.domain.payroom.entity.PaymentParticipant;
import com.podo.domain.payroom.entity.RoomMember;
import com.podo.domain.payroom.exception.NotRoomMemberException;
import com.podo.domain.payroom.exception.PayRoomNotFoundException;
import com.podo.domain.payroom.repository.AccountRepository;
import com.podo.domain.payroom.repository.PayRoomRepository;
import com.podo.domain.payroom.repository.PaymentParticipantRepository;
import com.podo.domain.payroom.repository.PaymentRepository;
import com.podo.domain.payroom.repository.RoomMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 방 상세 화면 전용 조회 서비스.
 * 화면 하나에 필요한 데이터를 한 곳에서 조회해, 결제 수와 상관없이 쿼리 수를 일정하게 유지한다.
 *
 * 1) 방               1회
 * 2) 멤버 + 사용자     1회 (fetch join) → 이 목록으로 멤버 여부도 확인
 * 3) 결제 + 결제자     1회 (fetch join)
 * 4) 참여자 + 사용자   1회 (IN + fetch join)  ← 결제마다 조회하던 N+1 제거
 * 5) 계좌 + 소유자     1회 (fetch join)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PayRoomDetailService {

    private final PayRoomRepository payRoomRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentParticipantRepository participantRepository;
    private final AccountRepository accountRepository;

    public PayRoomDetailResponse getDetail(Long roomId, Long userId) {
        PayRoom payRoom = payRoomRepository.findById(roomId)
                .orElseThrow(() -> new PayRoomNotFoundException(roomId));

        // 멤버 목록을 한 번에 가져와 멤버 확인까지 처리 (별도 exists 쿼리 없음)
        List<RoomMember> roomMembers = roomMemberRepository.findAllByPayRoomWithUser(payRoom);
        boolean isMember = roomMembers.stream()
                .anyMatch(rm -> rm.getUser().getId().equals(userId));
        if (!isMember) {
            throw new NotRoomMemberException();
        }

        List<MemberInfo> members = roomMembers.stream()
                .map(rm -> MemberInfo.from(rm, payRoom))
                .toList();

        List<Payment> payments = paymentRepository.findAllByPayRoomWithPayer(payRoom);
        Map<Long, List<PaymentParticipant>> participantsByPayment =
                participantRepository.findGroupedByPayment(payments);

        List<PaymentResponse> paymentResponses = payments.stream()
                .map(p -> PaymentResponse.from(p, participantsByPayment.getOrDefault(p.getId(), List.of())))
                .toList();

        List<AccountResponse> accounts = accountRepository.findAllByPayRoomWithUser(payRoom).stream()
                .map(AccountResponse::from)
                .toList();

        boolean isOwner = payRoom.getOwner().getId().equals(userId);

        return new PayRoomDetailResponse(
                PayRoomResponse.from(payRoom), members, paymentResponses, accounts, isOwner);
    }
}