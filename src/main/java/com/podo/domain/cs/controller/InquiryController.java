package com.podo.domain.cs.controller;

import com.podo.domain.cs.dto.request.InquiryCreateRequest;
import com.podo.domain.cs.dto.request.InquiryUpdateRequest;
import com.podo.domain.cs.dto.response.InquiryListResponse;
import com.podo.domain.cs.dto.response.InquiryResponse;
import com.podo.domain.cs.service.InquiryService;
import com.podo.global.exception.BusinessException;
import com.podo.global.security.PrincipalUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/inquiries")
@RequiredArgsConstructor
public class InquiryController {

    private final InquiryService inquiryService;

    @GetMapping
    public String list(@RequestParam(required = false) String keyword,
                       @RequestParam(defaultValue = "0") int page,
                       @AuthenticationPrincipal PrincipalUser principal,
                       Model model) {
        Pageable pageable = PageRequest.of(page, 10);
        Page<InquiryListResponse> inquiries = inquiryService.findMyInquiries(principal.getUserId(), keyword, pageable);

        model.addAttribute("inquiries", inquiries);
        model.addAttribute("keyword", keyword);

        return "inquiry/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("inquiryCreateRequest", new InquiryCreateRequest());
        return "inquiry/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute InquiryCreateRequest request,
                         BindingResult bindingResult,
                         @AuthenticationPrincipal PrincipalUser principal) {
        if (bindingResult.hasErrors()) {
            return "inquiry/form";
        }

        InquiryResponse inquiry = inquiryService.create(principal.getUserId(), request);
        return "redirect:/inquiries/" + inquiry.getId();
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id,
                         @AuthenticationPrincipal PrincipalUser principal,
                         Model model) {
        InquiryResponse inquiry = inquiryService.findById(id, principal.getUserId());
        model.addAttribute("inquiry", inquiry);
        return "inquiry/detail";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id,
                           @AuthenticationPrincipal PrincipalUser principal,
                           Model model) {
        InquiryResponse inquiry = inquiryService.findById(id, principal.getUserId());

        InquiryUpdateRequest request = new InquiryUpdateRequest();
        request.setTitle(inquiry.getTitle());
        request.setContent(inquiry.getContent());

        model.addAttribute("inquiry", inquiry);
        model.addAttribute("inquiryUpdateRequest", request);
        return "inquiry/edit-form";
    }

    @PostMapping("/{id}/edit")
    public String edit(@PathVariable Long id,
                       @Valid @ModelAttribute InquiryUpdateRequest request,
                       BindingResult bindingResult,
                       @AuthenticationPrincipal PrincipalUser principal,
                       Model model,
                       RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            InquiryResponse inquiry = inquiryService.findById(id, principal.getUserId());
            model.addAttribute("inquiry", inquiry);
            return "inquiry/edit-form";
        }

        try {
            inquiryService.update(id, principal.getUserId(), request);
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("editError", e.getMessage());
            return "redirect:/inquiries/" + id + "/edit";
        }

        return "redirect:/inquiries/" + id;
    }
}
