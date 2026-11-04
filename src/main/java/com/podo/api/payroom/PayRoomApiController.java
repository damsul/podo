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
        List<PayRoomResponse> rooms = payRoomService.findMyRooms(principal.getUserId());
        return ApiResponse.success(rooms);
    }

    @GetMapping("/{roomId}")
    public ApiResponse<PayRoomResponse> getRoom(@PathVariable Long roomId) {
        PayRoomResponse room = payRoomService.findById(roomId);
        return ApiResponse.success(room);
    }

    @GetMapping("/{roomId}/members")
    public ApiResponse<List<MemberInfo>> getMembers(@PathVariable Long roomId) {
        List<MemberInfo> members = payRoomService.findMembers(roomId);
        return ApiResponse.success(members);
    }

    @PostMapping
    public ApiResponse<PayRoomResponse> createRoom(
            @Valid @RequestBody PayRoomCreateRequest request,
            @AuthenticationPrincipal PrincipalUser principal
    ) {
        PayRoomResponse room = payRoomService.create(principal.getUserId(), request);
        return ApiResponse.success(room);
    }

    @PutMapping("/{roomId}")
    public ApiResponse<PayRoomResponse> updateRoom(
            @PathVariable Long roomId,
            @Valid @RequestBody PayRoomUpdateRequest request,
            @AuthenticationPrincipal PrincipalUser principal
    ) {
        PayRoomResponse room = payRoomService.update(roomId, principal.getUserId(), request);
        return ApiResponse.success(room);
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
        PayRoomResponse room = payRoomService.complete(roomId, principal.getUserId());
        return ApiResponse.success(room);
    }

    @PatchMapping("/{roomId}/reopen")
    public ApiResponse<PayRoomResponse> reopenRoom(
            @PathVariable Long roomId,
            @AuthenticationPrincipal PrincipalUser principal
    ) {
        PayRoomResponse room = payRoomService.reopen(roomId, principal.getUserId());
        return ApiResponse.success(room);
    }
}
