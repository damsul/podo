package com.podo.domain.admin.controller;

import com.podo.domain.user.dto.request.AdminPasswordChangeRequest;
import com.podo.domain.user.dto.request.AdminUserCreateRequest;
import com.podo.domain.user.dto.request.AdminUserUpdateRequest;
import com.podo.domain.user.dto.response.AdminUserDetailResponse;
import com.podo.domain.user.entity.Role;
import com.podo.domain.user.exception.DuplicateEmailException;
import com.podo.domain.user.exception.DuplicateLoginIdException;
import com.podo.domain.user.service.UserService;
import com.podo.global.exception.BusinessException;
import com.podo.global.exception.ErrorCode;
import com.podo.global.exception.ImageUploadException;
import com.podo.global.exception.InvalidImageException;
import com.podo.global.security.PrincipalUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/admins")
@RequiredArgsConstructor
public class AdminAccountController {

    private final UserService userService;

    @GetMapping
    public String list(@RequestParam(required = false) Boolean deleted,
                       @RequestParam(required = false) String keyword,
                       @RequestParam(defaultValue = "0") int page,
                       @AuthenticationPrincipal PrincipalUser principal,
                       Model model) {
        PageRequest pageable = PageRequest.of(page, 20, Sort.by(Sort.Direction.DESC, "id"));

        model.addAttribute("users", userService.findAllForAdmin(Role.ADMIN, deleted, keyword, pageable));
        model.addAttribute("deleted", deleted);
        model.addAttribute("keyword", keyword);
        model.addAttribute("loginUserId", principal.getUser().getId());
        return "admin/admins/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id,
                         @AuthenticationPrincipal PrincipalUser principal,
                         Model model) {
        model.addAttribute("user", userService.getDetailForAdmin(Role.ADMIN, id));
        model.addAttribute("loginUserId", principal.getUser().getId());
        return "admin/admins/detail";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("form", new AdminUserCreateRequest());
        return "admin/admins/new";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") AdminUserCreateRequest form,
                         BindingResult bindingResult) {
        if(bindingResult.hasErrors()) return "admin/admins/new";
        try {
            Long id = userService.createForAdmin(Role.ADMIN, form);
            return "redirect:/admin/admins/" + id;
        } catch (DuplicateLoginIdException | DuplicateEmailException e) {
            rejectDuplicate(e, bindingResult);
        }
        return "admin/admins/new";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id,
                           @AuthenticationPrincipal PrincipalUser principal,
                           Model model) {
        AdminUserDetailResponse user = userService.getDetailForAdmin(Role.ADMIN, id);
        model.addAttribute("user", user);
        model.addAttribute("form", AdminUserUpdateRequest.from(user));
        model.addAttribute("loginUserId", principal.getUser().getId());
        return "admin/admins/edit";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("form") AdminUserUpdateRequest form,
                         BindingResult bindingResult,
                         @AuthenticationPrincipal PrincipalUser principal,
                         Model model) {
        if (!bindingResult.hasErrors()) {
            try {
                userService.updateForAdmin(Role.ADMIN, id, form);
                return "redirect:/admin/admins/" + id;
            } catch (DuplicateEmailException e) {
                rejectDuplicate(e, bindingResult);
            }
        }
        model.addAttribute("user", userService.getDetailForAdmin(Role.ADMIN, id));
        model.addAttribute("loginUserId", principal.getUser().getId());
        return "admin/admins/edit";
    }

    @PostMapping("/{id}/password")
    public String changePassword(@PathVariable Long id,
                                 @Valid @ModelAttribute AdminPasswordChangeRequest request,
                                 BindingResult bindingResult,
                                 RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("passwordError",
                    bindingResult.getAllErrors().get(0).getDefaultMessage());
        } else {
            userService.changePasswordForAdmin(Role.ADMIN, id, request);
            redirectAttributes.addFlashAttribute("message", "비밀번호를 변경했어요");
        }
        return "redirect:/admin/admins/" + id + "/edit";
    }

    @PostMapping("/{id}/withdraw")
    public String withdraw(@PathVariable Long id, @AuthenticationPrincipal PrincipalUser principal) {
        userService.withdrawAdminAccount(id, principal.getUser().getId());
        return "redirect:/admin/admins/" + id;

    }

    private void rejectDuplicate(BusinessException e, BindingResult bindingResult) {
        ErrorCode errorCode = e.getErrorCode();
        String field = (errorCode == ErrorCode.DUPLICATE_LOGIN_ID) ? "loginId" : "email";
        bindingResult.rejectValue(field, "duplicate", errorCode.getMessage());
    }

    @PostMapping("/{id}/profile-image")
    public String changeProfileImage(@PathVariable Long id,
                                     @RequestParam("profileImage") MultipartFile profileImage,
                                     RedirectAttributes redirectAttributes) {
        try {
            userService.updateProfileImageForAdmin(Role.ADMIN, id, profileImage, false);
            redirectAttributes.addFlashAttribute("message", "프로필 사진을 변경했어요");
        } catch (InvalidImageException | ImageUploadException e) {
            redirectAttributes.addFlashAttribute("imageError", e.getMessage());
        }
        return "redirect:/admin/admins/" + id + "/edit";
    }

    @PostMapping("/{id}/profile-image/delete")
    public String deleteProfileImage(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        userService.updateProfileImageForAdmin(Role.ADMIN, id, null, true);
        redirectAttributes.addFlashAttribute("message", "프로필 사진을 삭제했어요");
        return "redirect:/admin/admins/" + id + "/edit";
    }

}
