package com.podo.api.payroom;

import com.podo.domain.payroom.dto.request.AccountCreateRequest;
import com.podo.domain.payroom.dto.request.AccountUpdateRequest;
import com.podo.domain.payroom.dto.response.AccountResponse;
import com.podo.domain.payroom.service.AccountService;
import com.podo.global.common.ApiResponse;
import com.podo.global.security.PrincipalUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rooms/{roomId}/accounts")
@RequiredArgsConstructor
public class AccountApiController {

    private final AccountService accountService;

    @PostMapping
    public ApiResponse<AccountResponse> create(
            @PathVariable Long roomId,
            @AuthenticationPrincipal PrincipalUser principal,
            @Valid @RequestBody AccountCreateRequest request) {
        return ApiResponse.success(accountService.create(roomId, principal.getUserId(), request));
    }

    @GetMapping
    public ApiResponse<List<AccountResponse>> findByRoom(
            @PathVariable Long roomId,
            @AuthenticationPrincipal PrincipalUser principal
    ) {
        return ApiResponse.success(accountService.findByRoom(roomId, principal.getUserId()));
    }

    @PutMapping("/{accountId}")
    public ApiResponse<AccountResponse> update(
            @PathVariable Long roomId,
            @PathVariable Long accountId,
            @AuthenticationPrincipal PrincipalUser principal,
            @Valid @RequestBody AccountUpdateRequest request
    ) {
        return ApiResponse.success(accountService.update(roomId, accountId, principal.getUserId(), request));
    }

    @DeleteMapping("/{accountId}")
    public ApiResponse<Void> delete(
            @PathVariable Long roomId,
            @PathVariable Long accountId,
            @AuthenticationPrincipal PrincipalUser principal
    ) {
        accountService.delete(roomId, accountId, principal.getUserId());
        return ApiResponse.success();
    }
}
