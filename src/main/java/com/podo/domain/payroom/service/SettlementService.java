package com.podo.domain.payroom.service;

import com.podo.domain.payroom.dto.response.AccountResponse;
import com.podo.domain.payroom.dto.response.SettlementResponse;
import com.podo.domain.payroom.dto.response.SettlementResponse.TransferInfo;
import com.podo.domain.payroom.entity.PayRoom;
import com.podo.domain.payroom.entity.Payment;
import com.podo.domain.payroom.entity.PaymentParticipant;
import com.podo.domain.payroom.exception.NotRoomMemberException;
import com.podo.domain.payroom.exception.PayRoomNotFoundException;
import com.podo.domain.payroom.repository.AccountRepository;
import com.podo.domain.payroom.repository.PayRoomRepository;
import com.podo.domain.payroom.repository.PaymentParticipantRepository;
import com.podo.domain.payroom.repository.PaymentRepository;
import com.podo.domain.payroom.repository.RoomMemberRepository;
import com.podo.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.podo.domain.payroom.dto.response.SettlementResponse.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SettlementService {

    private final PayRoomRepository payRoomRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentParticipantRepository participantRepository;
    private final AccountRepository accountRepository;
    private final RoomMemberRepository roomMemberRepository;

    // 정산 결과는 저장하지 않고 요청할 때마다 계산 (방 멤버만 조회 가능)
    // 관리자 화면은 방장 id를 넘겨 호출하므로 이 검사를 통과한다
    // 쿼리: 방 1 + 멤버 확인 1 + 결제(결제자 포함) 1 + 참여자(사용자 포함) 1 → 결제 수와 무관
    public SettlementResponse calculate(Long roomId, Long userId) {
        PayRoom payRoom = findPayRoomById(roomId);
        validateRoomMember(roomId, userId);

        boolean isOwner = payRoom.getOwner().getId().equals(userId);
        int roundUnit = payRoom.getRoundUnit();

        List<Payment> payments = paymentRepository.findAllByPayRoomWithPayer(payRoom);
        Map<Long, List<PaymentParticipant>> participantsByPayment =
                participantRepository.findGroupedByPayment(payments);

        List<TransferInfo> transfers = new ArrayList<>();
        Map<String, NetAccumulator> netMap = new LinkedHashMap<>();
        int totalAmount = 0;

        for (Payment payment : payments) {
            totalAmount += payment.getAmount();
            List<PaymentParticipant> participants =
                    participantsByPayment.getOrDefault(payment.getId(), List.of());
            User payer = payment.getPayer();
            if (participants.isEmpty()) continue;

            int rawPerPerson = payment.getAmount() / participants.size();
            int roundedPerPerson = (rawPerPerson / roundUnit) * roundUnit;

            for (PaymentParticipant participant : participants) {
                User pu = participant.getUser();
                if (pu.getId().equals(payer.getId())) continue;

                transfers.add(TransferInfo.builder()
                        .fromUserId(pu.getId()).fromUserName(pu.getName())
                        .toUserId(payer.getId()).toUserName(payer.getName())
                        .amount(roundedPerPerson)
                        .rawAmount(rawPerPerson)
                        .paymentId(payment.getId()).paymentTitle(payment.getTitle())
                        .participantId(participant.getId())
                        .settled(participant.isSettled())
                        .canSettle(isOwner || payer.getId().equals(userId))   // 방장 또는 받는 사람
                        .build());

                String key = pu.getId() + "->" + payer.getId();
                NetAccumulator acc = netMap.computeIfAbsent(key, k -> new NetAccumulator(pu, payer));
                acc.rawTotal += rawPerPerson;
                acc.participantIds.add(participant.getId());
                acc.allSettled = acc.allSettled && participant.isSettled();
            }
        }

        List<NetTransferInfo> netTransfers = netMap.values().stream()
                .map(acc -> NetTransferInfo.builder()
                        .fromUserId(acc.fromUser.getId()).fromUserName(acc.fromUser.getName())
                        .toUserId(acc.toUser.getId()).toUserName(acc.toUser.getName())
                        .rawAmount(acc.rawTotal)
                        .amount((acc.rawTotal / roundUnit) * roundUnit)
                        .settled(acc.allSettled)
                        .canSettle(isOwner || acc.toUser.getId().equals(userId))   // 방장 또는 받는 사람
                        .participantIds(acc.participantIds)
                        .build())
                .toList();

        return SettlementResponse.builder()
                .roomId(payRoom.getId()).roomTitle(payRoom.getTitle())
                .totalAmount(totalAmount).roundUnit(roundUnit)
                .isRoomOwner(isOwner)
                .transfers(transfers).netTransfers(netTransfers)
                .build();
    }

    // API용: 정산 계산과 계좌 조회를 직접 해서 공유 문구를 만든다
    public String generateShareText(Long roomId, Long userId) {
        SettlementResponse settlement = calculate(roomId, userId);
        List<AccountResponse> accounts = accountRepository.findAllByPayRoomWithUser(findPayRoomById(roomId)).stream()
                .map(AccountResponse::from)
                .toList();
        return buildShareText(settlement, accounts);
    }

    // 화면용: 이미 계산한 정산 결과와 계좌로 문구만 만든다 (DB 조회 없음)
    // 예전에는 정산 화면에서 calculate가 두 번 실행되던 것을 이걸로 한 번으로 줄였다
    public String buildShareText(SettlementResponse settlement, List<AccountResponse> accounts) {
        StringBuilder sb = new StringBuilder();
        sb.append("'").append(settlement.getRoomTitle()).append("' 모임 정산 내용입니다.\n\n[정산내용]\n");
        for (NetTransferInfo t : settlement.getNetTransfers()) {
            sb.append(t.getFromUserName()).append("님은 ").append(t.getToUserName())
                    .append("님에게 ").append(String.format("%,d", t.getAmount())).append("원 입금 부탁드립니다.\n");
        }
        if (!accounts.isEmpty()) {
            sb.append("\n[계좌정보]\n");
            for (AccountResponse a : accounts) {
                sb.append(a.getHolderName()).append("에게 보내실 분은 ")
                        .append(a.getBank()).append(" ").append(a.getAccountNumber()).append("로 보내주세요.\n");
            }
        }
        return sb.toString();
    }

    private PayRoom findPayRoomById(Long roomId) {
        return payRoomRepository.findById(roomId)
                .orElseThrow(() -> new PayRoomNotFoundException(roomId));
    }

    // 사용자 엔티티를 불러오지 않고 id만으로 멤버 여부 확인
    private void validateRoomMember(Long roomId, Long userId) {
        if (!roomMemberRepository.existsMember(roomId, userId)) {
            throw new NotRoomMemberException();
        }
    }

    private static class NetAccumulator {
        User fromUser;
        User toUser;
        int rawTotal = 0;
        boolean allSettled = true;
        List<Long> participantIds = new ArrayList<>();

        public NetAccumulator(User fromUser, User toUser) {
            this.fromUser = fromUser;
            this.toUser = toUser;
        }
    }
}