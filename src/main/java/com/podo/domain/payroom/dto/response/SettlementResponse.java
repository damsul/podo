package com.podo.domain.payroom.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class SettlementResponse {

    private Long roomId;
    private String roomTitle;
    private int totalAmount;
    private int roundUnit;
    private boolean isRoomOwner;
    private List<TransferInfo> transfers;
    private List<NetTransferInfo> netTransfers;

    @Getter
    @Builder
    public static class TransferInfo {
        private Long fromUserId;
        private String fromUserName;
        private Long toUserId;
        private String toUserName;
        private int amount;
        private int rawAmount;
        private Long paymentId;
        private String paymentTitle;
        private Long participantId;
        private boolean settled;
        private boolean canSettle;
    }

    @Getter
    @Builder
    public static class NetTransferInfo {
        private Long fromUserId;
        private String fromUserName;
        private Long toUserId;
        private String toUserName;
        private int amount;
        private int rawAmount;
        private List<Long> participantIds;
        private boolean settled;
        private boolean canSettle;
        private boolean zeroBalance;
    }
}
