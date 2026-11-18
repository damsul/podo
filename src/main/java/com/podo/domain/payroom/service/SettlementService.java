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
        Map<String, PairAccumulator> pairMap = new LinkedHashMap<>();
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

                // 방향과 상관없이 "두 사람" 단위로 모은다 (A→B, B→A가 같은 묶음)
                PairAccumulator acc = pairMap.computeIfAbsent(pairKey(pu, payer), k -> new PairAccumulator(pu, payer));
                acc.add(pu, rawPerPerson, participant);
            }
        }

        List<NetTransferInfo> netTransfers = pairMap.values().stream()
                .map(acc -> acc.toNetTransfer(roundUnit, isOwner, userId))
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
            if (t.isZeroBalance()) continue;   // 주고받을 돈이 없는 사이는 문구에서 제외
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

    // 두 사람을 id 순서로 묶은 키: (2,5)와 (5,2)가 같은 키가 된다
    private static String pairKey(User a, User b) {
        long low = Math.min(a.getId(), b.getId());
        long high = Math.max(a.getId(), b.getId());
        return low + "-" + high;
    }

    /**
     * 두 사람 사이의 송금을 양방향 모두 모은 뒤 차액만 남긴다 (상계)
     * - 금액은 절사 전 금액(raw)끼리 빼고, 마지막에 한 번만 절사한다
     * - 묶음에는 양방향 참여자 id가 모두 들어간다 (정산 완료 시 함께 처리)
     */
    private static class PairAccumulator {
        private final User low;    // id가 작은 사람
        private final User high;   // id가 큰 사람
        private int lowOwesHigh = 0;
        private int highOwesLow = 0;
        private boolean allSettled = true;
        private final List<Long> participantIds = new ArrayList<>();

        PairAccumulator(User a, User b) {
            this.low = a.getId() < b.getId() ? a : b;
            this.high = a.getId() < b.getId() ? b : a;
        }

        void add(User from, int raw, PaymentParticipant participant) {
            if (from.getId().equals(low.getId())) {
                lowOwesHigh += raw;
            } else {
                highOwesLow += raw;
            }
            participantIds.add(participant.getId());
            allSettled = allSettled && participant.isSettled();
        }

        NetTransferInfo toNetTransfer(int roundUnit, boolean isOwner, Long userId) {
            int diff = lowOwesHigh - highOwesLow;
            User from = diff >= 0 ? low : high;
            User to = diff >= 0 ? high : low;
            int raw = Math.abs(diff);
            int amount = (raw / roundUnit) * roundUnit;
            boolean zeroBalance = amount == 0;   // 상계 후 보낼 돈이 없음

            return NetTransferInfo.builder()
                    .fromUserId(from.getId()).fromUserName(from.getName())
                    .toUserId(to.getId()).toUserName(to.getName())
                    .rawAmount(raw)
                    .amount(amount)
                    .settled(allSettled)
                    .zeroBalance(zeroBalance)
                    .canSettle(!zeroBalance && (isOwner || to.getId().equals(userId)))   // 방장 또는 최종 받는 사람
                    .participantIds(participantIds)
                    .build();
        }
    }
}