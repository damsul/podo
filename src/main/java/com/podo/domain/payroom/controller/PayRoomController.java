package com.podo.domain.payroom.controller;

import com.podo.domain.payroom.dto.request.AddMemberRequest;
import com.podo.domain.payroom.dto.request.PayRoomCreateRequest;
import com.podo.domain.payroom.dto.response.*;
import com.podo.domain.payroom.service.AccountService;
import com.podo.domain.payroom.service.PayRoomDetailService;
import com.podo.domain.payroom.service.PayRoomService;
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
    private final PayRoomDetailService payRoomDetailService;
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

        payRoomService.create(principal.getUserId(), request);

        return "redirect:/";
    }

    @GetMapping("/{roomId}")
    public String detail(@PathVariable Long roomId,
                         @AuthenticationPrincipal PrincipalUser principal,
                         Model model) {
        Long userId = principal.getUserId();

        // 화면 전용 조회 서비스 한 번 호출 (멤버 확인 1회, 결제 수와 무관하게 쿼리 수 고정)
        PayRoomDetailResponse detail = payRoomDetailService.getDetail(roomId, userId);

        model.addAttribute("room", detail.getRoom());
        model.addAttribute("members", detail.getMembers());
        model.addAttribute("payments", detail.getPayments());
        model.addAttribute("accounts", detail.getAccounts());
        model.addAttribute("isOwner", detail.isOwner());
        model.addAttribute("currentUserId", userId);

        return "room/detail";
    }

    @GetMapping("/{roomId}/settlement")
    public String settlement(@PathVariable Long roomId,
                             @AuthenticationPrincipal PrincipalUser principal,
                             Model model) {
        Long userId = principal.getUserId();

        SettlementResponse settlement = settlementService.calculate(roomId, userId);
        List<AccountResponse> accounts = accountService.findByRoom(roomId, userId);
        // 이미 계산한 결과로 공유 문구만 만든다 (정산을 다시 계산하지 않음)
        String shareText = settlementService.buildShareText(settlement, accounts);

        LinkedHashMap<String, List<TransferInfo>> grouped = settlement.getTransfers().stream()
                .collect(Collectors.groupingBy(
                        TransferInfo::getPaymentTitle,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        model.addAttribute("settlement", settlement);
        model.addAttribute("groupedTransfers", grouped);
        model.addAttribute("shareText", shareText);
        model.addAttribute("accounts", accounts);

        return "room/settlement";
    }

    // 정산 화면의 절사 옵션 변경 (방장만, 방 설정으로 저장)
    @PostMapping("/{roomId}/settlement/round-unit")
    public String changeRoundUnit(@PathVariable Long roomId,
                                  @RequestParam Integer roundUnit,
                                  @AuthenticationPrincipal PrincipalUser principal) {
        payRoomService.changeRoundUnit(roomId, principal.getUserId(), roundUnit);
        return "redirect:/rooms/" + roomId + "/settlement";
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