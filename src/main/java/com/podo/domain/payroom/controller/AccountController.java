package com.podo.domain.payroom.controller;

import com.podo.domain.payroom.dto.request.AccountCreateRequest;
import com.podo.domain.payroom.dto.request.AccountUpdateRequest;
import com.podo.domain.payroom.service.AccountService;
import com.podo.global.exception.BusinessException;
import com.podo.global.security.PrincipalUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/rooms/{roomId}/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    public String create(@PathVariable Long roomId,
                         @Valid @ModelAttribute AccountCreateRequest request,
                         BindingResult bindingResult,
                         @AuthenticationPrincipal PrincipalUser principal,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            String errorMessage = getFirstErrorInOrder(bindingResult, "userId", "bank", "accountNumber");
            redirectAttributes.addFlashAttribute("accountError", errorMessage);
            return "redirect:/rooms/" + roomId;
        }

        try {
            accountService.create(roomId, principal.getUserId(), request);
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("accountError", e.getMessage());
        }

        return "redirect:/rooms/" + roomId;
    }

    @PostMapping("/{accountId}")
    public String update(@PathVariable Long roomId,
                         @PathVariable Long accountId,
                         @Valid @ModelAttribute AccountUpdateRequest request,
                         BindingResult bindingResult,
                         @AuthenticationPrincipal PrincipalUser principal,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            String errorMessage = getFirstErrorInOrder(bindingResult, "bank", "accountNumber");
            redirectAttributes.addFlashAttribute("accountError", errorMessage);
            return "redirect:/rooms/" + roomId;
        }

        try {
            accountService.update(roomId, accountId, principal.getUserId(), request);
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("accountError", e.getMessage());
        }

        return "redirect:/rooms/" + roomId;
    }

    @PostMapping("/{accountId}/delete")
    public String delete(@PathVariable Long roomId,
                         @PathVariable Long accountId,
                         @AuthenticationPrincipal PrincipalUser principal,
                         RedirectAttributes redirectAttributes) {
        try {
            accountService.delete(roomId, accountId, principal.getUserId());
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("accountError", e.getMessage());
        }

        return "redirect:/rooms/" + roomId;
    }

    private String getFirstErrorInOrder(BindingResult bindingResult, String... fieldOrder) {
        for (String field : fieldOrder) {
            FieldError error = bindingResult.getFieldError(field);
            if (error != null) {
                return error.getDefaultMessage();
            }
        }
        return bindingResult.getAllErrors().getFirst().getDefaultMessage();
    }
}