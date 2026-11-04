package com.podo.api.payroom;

import com.podo.domain.payroom.dto.request.PaymentCreateRequest;
import com.podo.domain.payroom.dto.response.PaymentResponse;
import com.podo.domain.payroom.dto.request.PaymentUpdateRequest;
import com.podo.domain.payroom.service.PaymentService;
import com.podo.global.common.ApiResponse;
import com.podo.global.security.PrincipalUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rooms/{roomId}/payments")
@RequiredArgsConstructor
public class PaymentApiController {

    private final PaymentService paymentService;

    @GetMapping
    public ApiResponse<List<PaymentResponse>> getPayments(@PathVariable Long roomId) {
        List<PaymentResponse> rooms = paymentService.findByRoom(roomId);
        return ApiResponse.success(rooms);
    }

    @GetMapping("/{paymentId}")
    public ApiResponse<PaymentResponse> getPayment(@PathVariable Long roomId,
                                                   @PathVariable Long paymentId) {
        PaymentResponse payment = paymentService.findById(paymentId);
        return ApiResponse.success(payment);
    }

    @PostMapping
    public ApiResponse<PaymentResponse> createPayment(
            @PathVariable Long roomId,
            @Valid @RequestBody PaymentCreateRequest request,
            @AuthenticationPrincipal PrincipalUser principal
    ) {
        PaymentResponse payment = paymentService.create(roomId, principal.getUserId(), request);
        return ApiResponse.success(payment);
    }

    @PutMapping("/{paymentId}")
    public ApiResponse<PaymentResponse> updatePayment(
            @PathVariable Long roomId,
            @PathVariable Long paymentId,
            @Valid @RequestBody PaymentUpdateRequest request,
            @AuthenticationPrincipal PrincipalUser principal
            ) {
        PaymentResponse payment = paymentService.update(paymentId, principal.getUserId(), request);
        return ApiResponse.success(payment);
    }

    @DeleteMapping("/{paymentId}")
    public ApiResponse<Void> deletePayment(
            @PathVariable Long roomId,
            @PathVariable Long paymentId,
            @AuthenticationPrincipal PrincipalUser principal
    ) {
        paymentService.delete(paymentId, principal.getUserId());
        return ApiResponse.success();
    }
}
