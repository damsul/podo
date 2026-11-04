package com.podo.domain.payroom.controller;

import com.podo.domain.payroom.dto.request.AddMemberRequest;
import com.podo.domain.payroom.dto.request.PayRoomCreateRequest;
import com.podo.domain.payroom.dto.response.*;
import com.podo.domain.payroom.service.AccountService;
import com.podo.domain.payroom.service.PayRoomService;
import com.podo.domain.payroom.service.PaymentService;
import com.podo.domain.payroom.service.SettlementService;
import com.podo.global.security.PrincipalUser;
import com.podo.global.exception.BusinessException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

import static com.podo.domain.payroom.dto.response.SettlementResponse.*;

@Controller
@RequestMapping("/rooms")
@RequiredArgsConstructor
public class PayRoomController {

    private final PayRoomService payRoomService;
    private final PaymentService paymentService;
    private final SettlementService settlementService;
    private final AccountService accountService;

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("payRoomCreateRequest", new PayRoomCreateRequest());
        return "room/form";
    }

    @PostMapping("/new")
    public String create(@Valid @ModelAttribute PayRoomCreateRequest request,
                         BindingResult bindingResult,
                         @AuthenticationPrincipal PrincipalUser principal) {
        if (bindingResult.hasErrors()) {
            return "room/form";
        }

        Long userId = principal.getUserId();
        payRoomService.create(userId, request);

        return "redirect:/";
    }

    @GetMapping("/{roomId}")
    public String detail(@PathVariable Long roomId,
                         @AuthenticationPrincipal PrincipalUser principal,
                         Model model) {
        PayRoomResponse room = payRoomService.findById(roomId);
        List<MemberInfo> members = payRoomService.findMembers(roomId);
        List<PaymentResponse> payments = paymentService.findByRoom(roomId);
        List<AccountResponse> accounts = accountService.findByRoom(roomId);

        boolean isOwner = room.getOwnerId().equals(principal.getUserId());

        model.addAttribute("room", room);
        model.addAttribute("members", members);
        model.addAttribute("payments", payments);
        model.addAttribute("accounts", accounts);
        model.addAttribute("isOwner", isOwner);
        model.addAttribute("currentUserId", principal.getUserId());

        return "room/detail";
    }

    @GetMapping("/{roomId}/settlement")
    public String settlement(@PathVariable Long roomId,
                             @AuthenticationPrincipal PrincipalUser principal,
                             Model model) {
        SettlementResponse settlement = settlementService.calculate(roomId, principal.getUserId());
        LinkedHashMap<String, List<TransferInfo>> grouped = settlement.getTransfers().stream()
                .collect(Collectors.groupingBy(
                        TransferInfo::getPaymentTitle,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        String shareText = settlementService.generateShareText(roomId);
        List<AccountResponse> accounts = accountService.findByRoom(roomId);

        model.addAttribute("settlement", settlement);
        model.addAttribute("groupedTransfers", grouped);
        model.addAttribute("shareText", shareText);
        model.addAttribute("accounts", accounts);

        return "room/settlement";
    }

    @PostMapping("/{roomId}/members")
    public String addMember(@PathVariable Long roomId,
                            @Valid @ModelAttribute AddMemberRequest request,
                            BindingResult bindingResult,
                            @AuthenticationPrincipal PrincipalUser principal,
                            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("addMemberError",
                    bindingResult.getAllErrors().getFirst().getDefaultMessage());
            return "redirect:/rooms/" + roomId;
        }

        try {
            payRoomService.addMember(roomId, principal.getUserId(), request.getLoginId());
            redirectAttributes.addFlashAttribute("addMemberSuccess",
                    request.getLoginId() + "님을 추가했습니다");
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("addMemberError", e.getMessage());
        }

        return "redirect:/rooms/" + roomId;
    }

}
