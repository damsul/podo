package com.podo.domain.cs.controller;

import com.podo.domain.cs.service.FaqService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/faqs")
@RequiredArgsConstructor
public class FaqController {

    private final FaqService faqService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("faqs", faqService.findAll());
        return "faq/list";
    }
}
