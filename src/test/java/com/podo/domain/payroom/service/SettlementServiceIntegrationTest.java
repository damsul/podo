package com.podo.domain.payroom.service;

import com.podo.domain.payroom.dto.request.PayRoomUpdateRequest;
import com.podo.domain.payroom.dto.request.PaymentCreateRequest;
import com.podo.domain.payroom.dto.request.PayRoomCreateRequest;
import com.podo.domain.payroom.dto.response.PayRoomResponse;
import com.podo.domain.payroom.dto.response.SettlementResponse;
import com.podo.domain.user.entity.User;
import com.podo.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SettlementServiceIntegrationTest {

    @Autowired
    private SettlementService settlementService;

    @Autowired
    private PayRoomService payRoomService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private UserRepository userRepository;

    private User gyounghwan;
    private User minwook;
    private User juhwang;
    private User youngjun;
    private User sohyun;
    private Long roomId;

    @BeforeEach
    void setUp() {
        // 사용자 5명 생성
        gyounghwan = userRepository.save(User.builder()
                .loginId("gyounghwan").name("경환").build());
        minwook = userRepository.save(User.builder()
                .loginId("minwook").name("민욱").build());
        juhwang = userRepository.save(User.builder()
                .loginId("juhwang").name("주황").build());
        youngjun = userRepository.save(User.builder()
                .loginId("youngjun").name("영준").build());
        sohyun = userRepository.save(User.builder()
                .loginId("sohyun").name("소현").build());

        // 방 생성 (경환가 방장)
        PayRoomResponse room = payRoomService.create(gyounghwan.getId(),
                PayRoomCreateRequest.builder()
                        .title("Podo MT")
                        .roundUnit(10)
                        .build());
        roomId = room.getId();

        // 나머지 멤버 추가
        payRoomService.addMember(roomId, gyounghwan.getId(), minwook.getLoginId());
        payRoomService.addMember(roomId, gyounghwan.getId(), juhwang.getLoginId());
        payRoomService.addMember(roomId, gyounghwan.getId(), youngjun.getLoginId());
        payRoomService.addMember(roomId, gyounghwan.getId(), sohyun.getLoginId());
    }

    @Test
    @DisplayName("실제 시나리오: Podo MT 정산 계산 (이미지의 화면 시나리오)")
    void calculate_realScenario() {

        paymentService.create(roomId, gyounghwan.getId(), PaymentCreateRequest.builder()
                .title("장보기")
                .amount(80000)
                .payerId(gyounghwan.getId())
                .participantIds(List.of(
                        gyounghwan.getId(), minwook.getId(), juhwang.getId(),
                        youngjun.getId(), sohyun.getId()))
                .build());

        paymentService.create(roomId, sohyun.getId(), PaymentCreateRequest.builder()
                .title("야식")
                .amount(20000)
                .payerId(sohyun.getId())
                .participantIds(List.of(
                        sohyun.getId(), gyounghwan.getId(), youngjun.getId()))
                .build());

        SettlementResponse response = settlementService.calculate(roomId);

        assertThat(response.getRoomTitle()).isEqualTo("Podo MT");
        assertThat(response.getTotalAmount()).isEqualTo(100000);

        assertThat(response.getTransfers()).hasSize(6);

        long groceryCount = response.getTransfers().stream()
                .filter(t -> t.getPaymentTitle().equals("장보기"))
                .count();
        assertThat(groceryCount).isEqualTo(4);

        long lateNightCount = response.getTransfers().stream()
                .filter(t -> t.getPaymentTitle().equals("야식"))
                .count();
        assertThat(lateNightCount).isEqualTo(2);

        response.getTransfers().stream()
                .filter(t -> t.getPaymentTitle().equals("야식"))
                .forEach(t -> assertThat(t.getAmount()).isEqualTo(6660));
    }

    @Test
    @DisplayName("정산 완료된 항목은 settled=true로 표시된다")
    void calculate_reflectsSettledStatus() {
        paymentService.create(roomId, gyounghwan.getId(), PaymentCreateRequest.builder()
                .title("장보기")
                .amount(80000)
                .payerId(gyounghwan.getId())
                .participantIds(List.of(
                        gyounghwan.getId(), minwook.getId(), juhwang.getId()))
                .build());

        SettlementResponse before = settlementService.calculate(roomId);
        Long firstParticipantId = before.getTransfers().get(0).getParticipantId();

        paymentService.markAsSettled(firstParticipantId, gyounghwan.getId());

        SettlementResponse after = settlementService.calculate(roomId);

        long settledCount = after.getTransfers().stream()
                .filter(SettlementResponse.TransferInfo::isSettled)
                .count();
        assertThat(settledCount).isEqualTo(1);
    }

    @Test
    @DisplayName("절사 단위 100원으로 변경 후 재계산 - 6,660원이 6,600원으로")
    void calculate_changeRoundUnit() {
        paymentService.create(roomId, sohyun.getId(), PaymentCreateRequest.builder()
                .title("야식")
                .amount(20000)
                .payerId(sohyun.getId())
                .participantIds(List.of(
                        sohyun.getId(), gyounghwan.getId(), youngjun.getId()))
                .build());

        SettlementResponse before = settlementService.calculate(roomId);
        assertThat(before.getTransfers())
                .allMatch(t -> t.getAmount() == 6660);

        payRoomService.update(roomId, gyounghwan.getId(),
                PayRoomUpdateRequest.builder()
                        .title("Podo MT")
                        .roundUnit(100)
                        .build());

        SettlementResponse after = settlementService.calculate(roomId);

        assertThat(after.getTransfers())
                .allMatch(t -> t.getAmount() == 6600);
    }
}