package com.podo.domain.admin.controller;

import com.podo.domain.cs.dto.request.FaqCreateRequest;
import com.podo.domain.cs.dto.request.FaqUpdateRequest;
import com.podo.domain.cs.dto.response.FaqResponse;
import com.podo.domain.cs.service.FaqService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/faqs")
@RequiredArgsConstructor
public class AdminFaqController {

    private final FaqService faqService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("faqs", faqService.findAll());
        return "admin/faq/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("faqCreateRequest", new FaqCreateRequest());
        return "admin/faq/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute FaqCreateRequest request, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "admin/faq/form";
        }
        faqService.create(request);
        return "redirect:/admin/faqs";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        FaqResponse faq = faqService.findById(id);
        model.addAttribute("faq", faq);
        model.addAttribute("faqUpdateRequest", FaqUpdateRequest.from(faq));
        return "admin/faq/edit-form";
    }

    @PostMapping("/{id}/edit")
    public String edit(@PathVariable Long id,
                       @Valid @ModelAttribute FaqUpdateRequest request,
                       BindingResult bindingResult,
                       Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("faq", faqService.findById(id));
            return "admin/faq/edit-form";
        }
        faqService.update(id, request);
        return "redirect:/admin/faqs";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        faqService.delete(id);
        return "redirect:/admin/faqs";
    }
}


