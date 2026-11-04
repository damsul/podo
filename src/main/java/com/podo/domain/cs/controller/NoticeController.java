package com.podo.domain.cs.controller;

import com.podo.domain.cs.dto.response.NoticeListResponse;
import com.podo.domain.cs.dto.response.NoticeResponse;
import com.podo.domain.cs.service.NoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/notices")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    @GetMapping
    public String list(@RequestParam(required = false) String keyword,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        Pageable pageable = PageRequest.of(page, 10, Sort.by("id").descending());
        Page<NoticeListResponse> notices = noticeService.findAll(keyword, pageable);

        model.addAttribute("notices", notices);
        model.addAttribute("keyword", keyword);

        return "notice/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        NoticeResponse notice = noticeService.findById(id);
        model.addAttribute("notice", notice);
        return "notice/detail";
    }
}
