package com.podo.api.payroom;

import com.podo.domain.payroom.dto.response.SettlementResponse;
import com.podo.domain.payroom.service.PaymentService;
import com.podo.domain.payroom.service.SettlementService;
import com.podo.global.common.ApiResponse;
import com.podo.global.security.PrincipalUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rooms/{roomId}/settlement")
@RequiredArgsConstructor
public class SettlementApiController {

    private final SettlementService settlementService;
    private final PaymentService paymentService;

    @GetMapping
    public ApiResponse<SettlementResponse> getSettlement(
            @PathVariable Long roomId,
            @AuthenticationPrincipal PrincipalUser principal
    ) {
        return ApiResponse.success(settlementService.calculate(roomId, principal.getUserId()));
    }

    @PatchMapping("/participants/{participantId}/settle")
    public ApiResponse<Void> settle(
            @PathVariable Long roomId,
            @PathVariable Long participantId,
            @AuthenticationPrincipal PrincipalUser principal
    ) {
        paymentService.markAsSettled(participantId, principal.getUserId());
        return ApiResponse.success();
    }

    @PatchMapping("/participants/{participantId}/unsettle")
    public ApiResponse<Void> unsettle(
            @PathVariable Long roomId,
            @PathVariable Long participantId,
            @AuthenticationPrincipal PrincipalUser principal
    ) {
        paymentService.markAsUnsettled(participantId, principal.getUserId());
        return ApiResponse.success();
    }

    @GetMapping("/share")
    public ApiResponse<String> getShareText(
            @PathVariable Long roomId,
            @AuthenticationPrincipal PrincipalUser principal
    ) {
        return ApiResponse.success(settlementService.generateShareText(roomId, principal.getUserId()));
    }
}