package com.podo.domain.home.controller;

import com.podo.domain.payroom.dto.response.PayRoomResponse;
import com.podo.domain.payroom.service.PayRoomService;
import com.podo.global.security.PrincipalUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final PayRoomService payRoomService;

    @GetMapping("/login")
    public String loginForm() {
        return "login";
    }

    @GetMapping("/")
    public String home(@AuthenticationPrincipal PrincipalUser principal, Model model) {
        boolean isLoggedIn = principal != null;
        model.addAttribute("isLoggedIn", isLoggedIn);


        if (isLoggedIn) {
            Long userId = principal.getUserId();
            String userName = principal.getUser().getName();
            List<PayRoomResponse> rooms = payRoomService.findMyRooms(userId);

            model.addAttribute("userName", userName);
            model.addAttribute("rooms", rooms);
        }

        return "home";
    }
}
