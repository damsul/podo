package com.podo.domain.payroom.dto.response;

import com.podo.domain.payroom.entity.Account;
import com.podo.domain.payroom.entity.PayRoom;
import com.podo.domain.payroom.entity.Payment;
import com.podo.domain.payroom.entity.RoomMember;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class AdminPayRoomDetailResponse {

    private Long id;
    private Long ownerId;
    private String title;
    private String ownerName;
    private String ownerLoginId;
    private LocalDate dueDate;
    private int roundUnit;
    private LocalDateTime createdAt;
    private List<String> memberNames;
    private List<PaymentInfo> payments;
    private List<AccountInfo> accounts;

    @Getter
    @Builder
    public static class PaymentInfo {
        private Long id;
        private String title;
        private int amount;
        private LocalDate paidAt;
    }

    @Getter
    @Builder
    public static class AccountInfo {
        private String bank;
        private String accountNumber;
        private String holderName;
    }

    public static AdminPayRoomDetailResponse of(PayRoom payRoom, List<RoomMember> members,
                                                List<Payment> payments, List<Account> accounts) {

        List<String> memberNames = members.stream().map(m -> m.getUser().getName()).toList();
        List<PaymentInfo> paymentInfos = payments.stream().map(p -> PaymentInfo.builder().id(p.getId()).title(p.getTitle()).amount(p.getAmount()).paidAt(p.getPaidAt()).build()).toList();
        List<AccountInfo> accountInfos = accounts.stream().map(a -> AccountInfo.builder().bank(a.getBank()).accountNumber(a.getAccountNumber()).holderName(a.getHolderName()).build()).toList();

        return AdminPayRoomDetailResponse.builder()
                .id(payRoom.getId())
                .ownerId(payRoom.getOwner().getId())
                .title(payRoom.getTitle())
                .ownerName(payRoom.getOwner().getName())
                .ownerLoginId(payRoom.getOwner().getLoginId())
                .dueDate(payRoom.getDueDate())
                .roundUnit(payRoom.getRoundUnit())
                .createdAt(payRoom.getCreatedAt())
                .memberNames(memberNames)
                .payments(paymentInfos)
                .accounts(accountInfos)
                .build();
    }
}
