package com.podo.domain.payroom.service;

import com.podo.domain.payroom.dto.response.SettlementResponse;
import com.podo.domain.payroom.dto.response.SettlementResponse.TransferInfo;
import com.podo.domain.payroom.entity.Account;
import com.podo.domain.payroom.entity.PayRoom;
import com.podo.domain.payroom.entity.Payment;
import com.podo.domain.payroom.entity.PaymentParticipant;
import com.podo.domain.payroom.exception.PayRoomNotFoundException;
import com.podo.domain.payroom.repository.AccountRepository;
import com.podo.domain.payroom.repository.PayRoomRepository;
import com.podo.domain.payroom.repository.PaymentParticipantRepository;
import com.podo.domain.payroom.repository.PaymentRepository;
import com.podo.domain.user.entity.User;
import com.podo.domain.user.exception.UserNotFoundException;
import com.podo.domain.user.repository.UserRepository;
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
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;

    public SettlementResponse calculate(Long roomId, Long userId) {
        PayRoom payRoom = findPayRoomById(roomId);
        boolean isOwner = payRoom.isOwner(findUserById(userId));
        int roundUnit = payRoom.getRoundUnit();

        List<Payment> payments = paymentRepository.findByPayRoom(payRoom);
        List<TransferInfo> transfers = new ArrayList<>();
        Map<String, NetAccumulator> netMap = new LinkedHashMap<>();
        int totalAmount = 0;

        for (Payment payment : payments) {
            totalAmount += payment.getAmount();
            List<PaymentParticipant> participants = participantRepository.findByPayment(payment);
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

    public String generateShareText(Long roomId) {
        SettlementResponse settlement = calculateForShare(roomId);
        PayRoom payRoom = findPayRoomById(roomId);
        List<Account> accounts = accountRepository.findByPayRoom(payRoom);

        StringBuilder sb = new StringBuilder();
        sb.append("'").append(payRoom.getTitle()).append("' 모임 정산 내용입니다.\n\n[정산내용]\n");
        for (NetTransferInfo t : settlement.getNetTransfers()) {
            sb.append(t.getFromUserName()).append("님은 ").append(t.getToUserName())
                    .append("님에게 ").append(String.format("%,d", t.getAmount())).append("원 입금 부탁드립니다.\n");
        }
        if (!accounts.isEmpty()) {
            sb.append("\n[계좌정보]\n");
            for (Account a : accounts) {
                sb.append(a.getHolderName()).append("에게 보내실 분은 ")
                        .append(a.getBank()).append(" ").append(a.getAccountNumber()).append("로 보내주세요.\n");
            }
        }
        return sb.toString();
    }

    private SettlementResponse calculateForShare(Long roomId) {
        PayRoom payRoom = findPayRoomById(roomId);
        return calculate(roomId, payRoom.getOwner().getId());
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    private PayRoom findPayRoomById(Long roomId) {
        return payRoomRepository.findById(roomId)
                .orElseThrow(() -> new PayRoomNotFoundException(roomId));
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
