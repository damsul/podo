package com.podo.api.payroom;

import com.podo.domain.payroom.dto.request.AccountCreateRequest;
import com.podo.domain.payroom.dto.request.AccountUpdateRequest;
import com.podo.domain.payroom.dto.response.AccountResponse;
import com.podo.domain.payroom.service.AccountService;
import com.podo.global.common.ApiResponse;
import com.podo.global.security.PrincipalUser;
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
            @RequestBody AccountCreateRequest request) {
        return ApiResponse.success(accountService.create(roomId, principal.getUserId(), request));
    }

    @GetMapping
    public ApiResponse<List<AccountResponse>> findByRoom(@PathVariable Long roomId) {
        return ApiResponse.success(accountService.findByRoom(roomId));
    }

    @PutMapping("/{accountId}")
    public ApiResponse<AccountResponse> update(
            @PathVariable Long roomId,
            @PathVariable Long accountId,
            @AuthenticationPrincipal PrincipalUser principal,
            @RequestBody AccountUpdateRequest request
    ) {
        return ApiResponse.success(accountService.update(accountId, principal.getUserId(), request));
    }

    @DeleteMapping("/{accountId}")
    public ApiResponse<Void> delete(
            @PathVariable Long roomId,
            @PathVariable Long accountId,
            @AuthenticationPrincipal PrincipalUser principal
    ) {
        accountService.delete(accountId, principal.getUserId());
        return ApiResponse.success();
    }
}
