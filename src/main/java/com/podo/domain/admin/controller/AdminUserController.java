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
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;

    @GetMapping
    public String list(@RequestParam(required = false) Boolean deleted,
                       @RequestParam(required = false) String keyword,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        PageRequest pageable = PageRequest.of(page, 20, Sort.by(Sort.Direction.DESC, "id"));

        model.addAttribute("users", userService.findAllForAdmin(Role.USER, deleted, keyword, pageable));
        model.addAttribute("deleted", deleted);
        model.addAttribute("keyword", keyword);
        return "admin/users/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("user", userService.getDetailForAdmin(Role.USER, id));
        return "admin/users/detail";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("form", new AdminUserCreateRequest());
        return "admin/users/new";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") AdminUserCreateRequest form, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) return "admin/users/new";
        try {
            Long id = userService.createForAdmin(Role.USER, form);
            return "redirect:/admin/users/" + id;
        } catch (DuplicateLoginIdException | DuplicateEmailException e) {
            rejectDuplicate(e, bindingResult);
        }
        return "admin/users/new";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        AdminUserDetailResponse user = userService.getDetailForAdmin(Role.USER, id);
        model.addAttribute("user", user);
        model.addAttribute("form", AdminUserUpdateRequest.from(user));
        return "admin/users/edit";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("form") AdminUserUpdateRequest form,
                         BindingResult bindingResult,
                         Model model) {
        if (!bindingResult.hasErrors()) {
            try {
                userService.updateForAdmin(Role.USER, id, form);
                return "redirect:/admin/users/" + id;
            } catch (DuplicateEmailException e) {
                rejectDuplicate(e, bindingResult);
            }
        }
        model.addAttribute("user", userService.getDetailForAdmin(Role.USER, id));  // 화면 상단 정보 다시 채우기
        return "admin/users/edit";
    }

    // 중복 예외의 ErrorCode를 보고 어느 필드에 에러를 붙일지 결정
    private void rejectDuplicate(BusinessException e, BindingResult bindingResult) {
        ErrorCode errorCode = e.getErrorCode();
        String field = (errorCode == ErrorCode.DUPLICATE_LOGIN_ID) ? "loginId" : "email";
        bindingResult.rejectValue(field, "duplicate", errorCode.getMessage());
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
            userService.changePasswordForAdmin(Role.USER, id, request);
            redirectAttributes.addFlashAttribute("message", "비밀번호를 변경했어요");
        }
        return "redirect:/admin/users/" + id + "/edit";
    }

    @PostMapping("/{id}/withdraw")
    public String withdraw(@PathVariable Long id) {
        userService.withdrawForAdmin(Role.USER, id);
        return "redirect:/admin/users/" + id;
    }
    
    @GetMapping("/check-login-id")
    @ResponseBody
    public Map<String, Boolean> checkLoginId(@RequestParam String loginId) {
        return Map.of("available", userService.isLoginAvailable(loginId));
    }

    @GetMapping("/check-email")
    @ResponseBody
    public Map<String, Boolean> checkEmail(@RequestParam String email) {
        return Map.of("available", userService.isEmailAvailable(email));
    }

    @PostMapping("/{id}/profile-image")
    public String changeProfileImage(@PathVariable Long id,
                                     @RequestParam("profileImage") MultipartFile profileImage,
                                     RedirectAttributes redirectAttributes) {
        try {
            userService.updateProfileImageForAdmin(Role.USER, id, profileImage, false);
            redirectAttributes.addFlashAttribute("message", "프로필 사진을 변경했어요");
        } catch (InvalidImageException | ImageUploadException e) {
            redirectAttributes.addFlashAttribute("imageError", e.getMessage());
        }
        return "redirect:/admin/users/" + id + "/edit";
    }

    @PostMapping("/{id}/profile-image/delete")
    public String deleteProfileImage(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        userService.updateProfileImageForAdmin(Role.USER, id, null, true);
        redirectAttributes.addFlashAttribute("message", "프로필 사진을 삭제했어요");
        return "redirect:/admin/users/" + id + "/edit";
    }

}
