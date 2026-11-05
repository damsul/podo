package com.podo.api.payroom;

import com.podo.domain.payroom.dto.request.AddMemberRequest;
import com.podo.domain.payroom.dto.request.PayRoomCreateRequest;
import com.podo.domain.payroom.dto.request.PayRoomUpdateRequest;
import com.podo.domain.payroom.dto.response.MemberInfo;
import com.podo.domain.payroom.dto.response.PayRoomResponse;
import com.podo.domain.payroom.service.PayRoomService;
import com.podo.global.common.ApiResponse;
import com.podo.global.security.PrincipalUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class PayRoomApiController {

    private final PayRoomService payRoomService;

    @GetMapping
    public ApiResponse<List<PayRoomResponse>> getMyRooms(
            @AuthenticationPrincipal PrincipalUser principal
    ) {
        return ApiResponse.success(payRoomService.findMyRooms(principal.getUserId()));
    }

    @GetMapping("/{roomId}")
    public ApiResponse<PayRoomResponse> getRoom(
            @PathVariable Long roomId,
            @AuthenticationPrincipal PrincipalUser principal
    ) {
        return ApiResponse.success(payRoomService.findById(roomId, principal.getUserId()));
    }

    @GetMapping("/{roomId}/members")
    public ApiResponse<List<MemberInfo>> getMembers(
            @PathVariable Long roomId,
            @AuthenticationPrincipal PrincipalUser principal
    ) {
        return ApiResponse.success(payRoomService.findMembers(roomId, principal.getUserId()));
    }

    @PostMapping
    public ApiResponse<PayRoomResponse> createRoom(
            @Valid @RequestBody PayRoomCreateRequest request,
            @AuthenticationPrincipal PrincipalUser principal
    ) {
        return ApiResponse.success(payRoomService.create(principal.getUserId(), request));
    }

    @PutMapping("/{roomId}")
    public ApiResponse<PayRoomResponse> updateRoom(
            @PathVariable Long roomId,
            @Valid @RequestBody PayRoomUpdateRequest request,
            @AuthenticationPrincipal PrincipalUser principal
    ) {
        return ApiResponse.success(payRoomService.update(roomId, principal.getUserId(), request));
    }

    @PostMapping("/{roomId}/members")
    public ApiResponse<Void> addMember(
            @PathVariable Long roomId,
            @Valid @RequestBody AddMemberRequest request,
            @AuthenticationPrincipal PrincipalUser principal
    ) {
        payRoomService.addMember(roomId, principal.getUserId(), request.getLoginId());
        return ApiResponse.success();
    }

    @PatchMapping("/{roomId}/complete")
    public ApiResponse<PayRoomResponse> completeRoom(
            @PathVariable Long roomId,
            @AuthenticationPrincipal PrincipalUser principal
    ) {
        return ApiResponse.success(payRoomService.complete(roomId, principal.getUserId()));
    }

    @PatchMapping("/{roomId}/reopen")
    public ApiResponse<PayRoomResponse> reopenRoom(
            @PathVariable Long roomId,
            @AuthenticationPrincipal PrincipalUser principal
    ) {
        return ApiResponse.success(payRoomService.reopen(roomId, principal.getUserId()));
    }
}