package com.podo.domain.user.controller;

import com.podo.domain.user.dto.request.ProfileEditRequest;
import com.podo.domain.user.dto.request.SignupRequest;
import com.podo.domain.user.dto.response.UserResponse;
import com.podo.domain.user.service.UserService;
import com.podo.global.exception.BusinessException;
import com.podo.global.exception.ImageUploadException;
import com.podo.global.exception.InvalidImageException;
import com.podo.global.security.PrincipalUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/signup")
    public String signupForm(Model model) {
        model.addAttribute("signupRequest", new SignupRequest());
        return "user/signup";
    }

    @PostMapping("/signup")
    public String signup(
            @Valid @ModelAttribute SignupRequest signupRequest,
            BindingResult bindingResult,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            return "user/signup";
        }

        UserResponse response = userService.signup(signupRequest);
        model.addAttribute("userName", response.getName());

        return "user/signup-complete";
    }

    @GetMapping("/mypage")
    public String myPage(@AuthenticationPrincipal PrincipalUser principal, Model model) {
        UserResponse user = userService.getMyPage(principal.getUserId());
        model.addAttribute("user", user);
        return "user/mypage";
    }

    @GetMapping("/mypage/edit")
    public String editForm(@AuthenticationPrincipal PrincipalUser principal, Model model) {
        UserResponse user = userService.getMyPage(principal.getUserId());
        ProfileEditRequest form = new ProfileEditRequest();
        form.setEmail(user.getEmail());
        model.addAttribute("user", user);
        model.addAttribute("profileEditRequest", form);
        return "user/mypage-edit";
    }

    @PostMapping("/mypage/edit")
    public String edit(
            @Valid @ModelAttribute ProfileEditRequest request,
            BindingResult bindingResult,
            @RequestParam(value = "profileImage", required = false) MultipartFile profileImage,
            @AuthenticationPrincipal PrincipalUser principal,
            Model model
    ) {
        String password = request.getPassword();
        if (password != null && !password.isBlank()) {
            if (password.length() < 8 || password.length() > 20) {
                bindingResult.rejectValue("password", "invalid", "비밀번호는 8자 이상어야 합니다.");
            } else if (!password.equals(request.getPasswordConfirm())) {
                bindingResult.rejectValue("passwordConfirm", "mismatch", "새 비밀번호가 일치하지 않습니다.");
            }
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("user", userService.getMyPage(principal.getUserId()));
            return "user/mypage-edit";
        }

        try {
            UserResponse updated = userService.updateMyProfile(principal.getUserId(), request, profileImage);
            principal.getUser().updateProfile(principal.getUser().getName(), updated.getProfileImageUrl());
        } catch (InvalidImageException | ImageUploadException e) {
            model.addAttribute("imageError", e.getMessage());
            model.addAttribute("user", userService.getMyPage(principal.getUserId()));
            return "user/mypage-edit";
        } catch (BusinessException e) {
            model.addAttribute("editError", e.getMessage());
            model.addAttribute("user", userService.getMyPage(principal.getUserId()));
            return "user/mypage-edit";
        }

        return "redirect:/mypage";
    }
}
