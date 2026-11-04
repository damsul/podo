package com.podo.domain.admin.controller;

import com.podo.domain.cs.dto.request.InquiryAnswerRequest;
import com.podo.domain.cs.dto.response.AdminInquiryResponse;
import com.podo.domain.cs.entity.InquiryStatus;
import com.podo.domain.cs.service.InquiryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/inquiries")
@RequiredArgsConstructor
public class AdminInquiryController {

    private final InquiryService inquiryService;

    @GetMapping
    public String list(@RequestParam(required = false) InquiryStatus status,
                       @RequestParam(required = false) String keyword,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        PageRequest pageable = PageRequest.of(page, 20);

        model.addAttribute("inquiries", inquiryService.findAllForAdmin(status, keyword, pageable));
        model.addAttribute("status", status);
        model.addAttribute("keyword", keyword);
        return "admin/inquiry/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        AdminInquiryResponse inquiry = inquiryService.findByIdForAdmin(id);

        InquiryAnswerRequest request = new InquiryAnswerRequest();
        request.setAnswer(inquiry.getAnswer());

        model.addAttribute("inquiry", inquiry);
        model.addAttribute("inquiryAnswerRequest", request);
        return "admin/inquiry/detail";
    }

    @PostMapping("/{id}/answer")
    public String answer(@PathVariable Long id,
                         @Valid @ModelAttribute InquiryAnswerRequest request,
                         BindingResult bindingResult,
                         Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("inquiry", inquiryService.findByIdForAdmin(id));
            return "admin/inquiry/detail";
        }

        inquiryService.answer(id, request);
        return "redirect:/admin/inquiries/" + id;
    }
}
