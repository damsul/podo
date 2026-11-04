package com.podo.domain.admin.controller;

import com.podo.domain.payroom.dto.response.AdminPayRoomDetailResponse;
import com.podo.domain.payroom.dto.response.SettlementResponse;
import com.podo.domain.payroom.entity.RoomStatus;
import com.podo.domain.payroom.service.PayRoomService;
import com.podo.domain.payroom.service.SettlementService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.podo.domain.payroom.dto.response.SettlementResponse.*;

@Controller
@RequestMapping("/admin/rooms")
@RequiredArgsConstructor
public class AdminPayRoomController {

    private final PayRoomService payRoomService;
    private final SettlementService settlementService;

    @GetMapping
    public String list(@RequestParam(required = false) RoomStatus status,
                       @RequestParam(required = false) String keyword,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        PageRequest pageable = PageRequest.of(page, 20);

        model.addAttribute("rooms", payRoomService.findAllForAdmin(status, keyword, pageable));
        model.addAttribute("status", status);
        model.addAttribute("keyword", keyword);
        return "admin/rooms/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        AdminPayRoomDetailResponse room = payRoomService.getDetailForAdmin(id);
        model.addAttribute("room", room);
        return "admin/rooms/detail";
    }

    @GetMapping("/{roomId}/payments/{paymentId}/result")
    @ResponseBody
    public List<TransferInfo> paymentResult(@PathVariable Long roomId, @PathVariable Long paymentId) {
        AdminPayRoomDetailResponse room = payRoomService.getDetailForAdmin(roomId);
        SettlementResponse settlement = settlementService.calculate(roomId, room.getOwnerId());
        return settlement.getTransfers().stream()
                .filter(t -> t.getPaymentId().equals(paymentId))
                .toList();
    }
}
