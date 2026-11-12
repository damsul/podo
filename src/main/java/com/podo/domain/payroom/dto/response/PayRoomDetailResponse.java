package com.podo.domain.payroom.dto.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

// 방 상세 화면 한 장에 필요한 데이터 묶음
@Getter
@RequiredArgsConstructor
public class PayRoomDetailResponse {

    private final PayRoomResponse room;
    private final List<MemberInfo> members;
    private final List<PaymentResponse> payments;
    private final List<AccountResponse> accounts;
    private final boolean owner;
}