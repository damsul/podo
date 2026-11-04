package com.podo.domain.admin.controller;

import com.podo.domain.cs.dto.request.NoticeCreateRequest;
import com.podo.domain.cs.dto.request.NoticeUpdateRequest;
import com.podo.domain.cs.dto.response.NoticeListResponse;
import com.podo.domain.cs.dto.response.NoticeResponse;
import com.podo.domain.cs.service.NoticeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/notices")
@RequiredArgsConstructor
public class AdminNoticeController {

    private final NoticeService noticeService;

    @GetMapping
    public String list(@RequestParam(required = false) String keyword,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        PageRequest pageable = PageRequest.of(page, 20);
        Page<NoticeListResponse> notices = noticeService.findAll(keyword, pageable);

        model.addAttribute("notices", notices);
        model.addAttribute("keyword", keyword);

        return "admin/notice/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("noticeCreateRequest", new NoticeCreateRequest());
        return "admin/notice/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute NoticeCreateRequest request, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "admin/notice/form";
        }
        noticeService.create(request);
        return "redirect:/admin/notices";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        NoticeResponse notice = noticeService.findByIdForAdmin(id);

        NoticeUpdateRequest request = new NoticeUpdateRequest();
        request.setTitle(notice.getTitle());
        request.setContent(notice.getContent());
        request.setPinned(notice.isPinned());

        model.addAttribute("notice", notice);
        model.addAttribute("noticeUpdateRequest", request);
        return "admin/notice/edit-form";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        noticeService.delete(id);
        return "redirect:/admin/notices";
    }
}
