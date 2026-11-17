package com.podo.domain.payroom.controller;

import com.podo.domain.payroom.dto.request.PaymentCreateRequest;
import com.podo.domain.payroom.dto.request.PaymentUpdateRequest;
import com.podo.domain.payroom.dto.response.MemberInfo;
import com.podo.domain.payroom.dto.response.PayRoomResponse;
import com.podo.domain.payroom.dto.response.PaymentResponse;
import com.podo.domain.payroom.service.PayRoomService;
import com.podo.domain.payroom.service.PaymentService;
import com.podo.global.security.PrincipalUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/rooms/{roomId}/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final PayRoomService payRoomService;

    @GetMapping("/new")
    public String createForm(@PathVariable Long roomId,
                             @AuthenticationPrincipal PrincipalUser principal,
                             Model model) {
        addFormAttributes(roomId, principal.getUserId(), model);

        PaymentCreateRequest request = new PaymentCreateRequest();
        request.setPaidAt(LocalDate.now());
        request.setPayerId(principal.getUserId());
        model.addAttribute("paymentCreateRequest", request);

        return "payment/form";
    }

    @PostMapping("/new")
    public String create(@PathVariable Long roomId,
                         @Valid @ModelAttribute PaymentCreateRequest request,
                         BindingResult bindingResult,
                         @AuthenticationPrincipal PrincipalUser principal,
                         Model model) {
        if (bindingResult.hasErrors()) {
            addFormAttributes(roomId, principal.getUserId(), model);
            return "payment/form";
        }

        paymentService.create(roomId, principal.getUserId(), request);

        return "redirect:/rooms/" + roomId;
    }

    @PostMapping("/participants/{participantId}/settle")
    public String settle(@PathVariable Long roomId,
                         @PathVariable Long participantId,
                         @AuthenticationPrincipal PrincipalUser principal) {

        paymentService.markAsSettled(roomId, participantId, principal.getUserId());
        return "redirect:/rooms/" + roomId + "/settlement";
    }

    @PostMapping("/participants/{participantId}/unsettle")
    public String unsettle(@PathVariable Long roomId,
                           @PathVariable Long participantId,
                           @AuthenticationPrincipal PrincipalUser principal) {

        paymentService.markAsUnsettled(roomId, participantId, principal.getUserId());
        return "redirect:/rooms/" + roomId + "/settlement";
    }

    @PostMapping("/participants/settle-group")
    public String settleGroup(@PathVariable Long roomId,
                              @RequestParam List<Long> participantIds,
                              @AuthenticationPrincipal PrincipalUser principal) {
        paymentService.markGroupAsSettled(roomId, participantIds, principal.getUserId());
        return "redirect:/rooms/" + roomId + "/settlement";
    }

    @PostMapping("/participants/unsettle-group")
    public String unsettleGroup(@PathVariable Long roomId,
                                @RequestParam List<Long> participantIds,
                                @AuthenticationPrincipal PrincipalUser principal) {
        paymentService.markGroupAsUnsettled(roomId, participantIds, principal.getUserId());
        return "redirect:/rooms/" + roomId + "/settlement";
    }

    @GetMapping("/{paymentId}/edit")
    public String editForm(@PathVariable Long roomId,
                           @PathVariable Long paymentId,
                           @AuthenticationPrincipal PrincipalUser principal,
                           Model model) {
        addFormAttributes(roomId, principal.getUserId(), model);
        PaymentResponse payment = paymentService.findById(roomId, paymentId, principal.getUserId());

        PaymentUpdateRequest request = PaymentUpdateRequest.builder()
                .title(payment.getTitle())
                .amount(payment.getAmount())
                .paidAt(payment.getPaidAt())
                .payerId(payment.getPayerId())
                .participantIds(payment.getParticipants().stream()
                        .map(PaymentResponse.ParticipantInfo::getUserId)
                        .toList())
                .build();

        model.addAttribute("payment", payment);
        model.addAttribute("paymentUpdateRequest", request);

        return "payment/edit-form";
    }

    @PostMapping("/{paymentId}/edit")
    public String edit(@PathVariable Long roomId,
                       @PathVariable Long paymentId,
                       @Valid @ModelAttribute PaymentUpdateRequest request,
                       BindingResult bindingResult,
                       @AuthenticationPrincipal PrincipalUser principal,
                       Model model) {
        if (bindingResult.hasErrors()) {
            // 수정 화면으로 돌아가야 하므로 edit-form + payment 정보를 함께 넘긴다
            addFormAttributes(roomId, principal.getUserId(), model);
            model.addAttribute("payment", paymentService.findById(roomId, paymentId, principal.getUserId()));
            return "payment/edit-form";
        }

        paymentService.update(roomId, paymentId, principal.getUserId(), request);

        return "redirect:/rooms/" + roomId;
    }

    @PostMapping("/{paymentId}/delete")
    public String delete(@PathVariable Long roomId,
                         @PathVariable Long paymentId,
                         @AuthenticationPrincipal PrincipalUser principal) {
        paymentService.delete(roomId, paymentId, principal.getUserId());
        return "redirect:/rooms/" + roomId;
    }

    // 결제 추가·수정 폼에 공통으로 필요한 화면 데이터 (방 멤버만 조회 가능)
    private void addFormAttributes(Long roomId, Long userId, Model model) {
        PayRoomResponse room = payRoomService.findById(roomId, userId);
        List<MemberInfo> members = payRoomService.findMembers(roomId, userId);

        boolean isOwner = room.getOwnerId().equals(userId);
        String currentUserName = members.stream()
                .filter(m -> m.getUserId().equals(userId))
                .findFirst()
                .map(MemberInfo::getName)
                .orElse("");

        model.addAttribute("room", room);
        model.addAttribute("members", members);
        model.addAttribute("isOwner", isOwner);
        model.addAttribute("currentUserName", currentUserName);
        model.addAttribute("currentUserId", userId);
    }
}