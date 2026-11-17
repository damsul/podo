package com.podo.domain.payroom.service;

import com.podo.domain.payroom.dto.request.PayRoomCreateRequest;
import com.podo.domain.payroom.dto.request.PayRoomUpdateRequest;
import com.podo.domain.payroom.dto.request.PaymentCreateRequest;
import com.podo.domain.payroom.dto.response.SettlementResponse;
import com.podo.domain.payroom.dto.response.SettlementResponse.NetTransferInfo;
import com.podo.domain.payroom.dto.response.SettlementResponse.TransferInfo;
import com.podo.domain.payroom.exception.NotRoomMemberException;
import com.podo.domain.user.entity.User;
import com.podo.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SettlementService 통합 테스트 (H2 + 실제 JPA)
 * - 방 생성 → 멤버 추가 → 결제 추가 → 정산 계산까지 실제 흐름으로 확인한다.
 * - 경환이 방장, 5명이 모두 방 멤버
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SettlementServiceIntegrationTest {

    @Autowired private SettlementService settlementService;
    @Autowired private PayRoomService payRoomService;
    @Autowired private PaymentService paymentService;
    @Autowired private UserRepository userRepository;

    private User gyounghwan;
    private User minwook;
    private User juhwang;
    private User youngjun;
    private User sohyun;
    private Long roomId;

    @BeforeEach
    void setUp() {
        gyounghwan = userRepository.save(User.builder().loginId("gyounghwan").name("경환").build());
        minwook = userRepository.save(User.builder().loginId("minwook").name("민욱").build());
        juhwang = userRepository.save(User.builder().loginId("juhwang").name("주황").build());
        youngjun = userRepository.save(User.builder().loginId("youngjun").name("영준").build());
        sohyun = userRepository.save(User.builder().loginId("sohyun").name("소현").build());

        roomId = payRoomService.create(gyounghwan.getId(),
                PayRoomCreateRequest.builder().title("Podo MT").roundUnit(10).build()).getId();

        for (User u : List.of(minwook, juhwang, youngjun, sohyun)) {
            payRoomService.addMember(roomId, gyounghwan.getId(), u.getLoginId());
        }
    }

    @Test
    @DisplayName("실제 시나리오: 장보기 80,000원(5명) + 야식 20,000원(3명)")
    void calculate_realScenario() {
        pay(gyounghwan, "장보기", 80000, gyounghwan, minwook, juhwang, youngjun, sohyun);
        pay(sohyun, "야식", 20000, sohyun, gyounghwan, youngjun);

        SettlementResponse response = settlementService.calculate(roomId, gyounghwan.getId());

        assertThat(response.getRoomTitle()).isEqualTo("Podo MT");
        assertThat(response.getTotalAmount()).isEqualTo(100000);
        assertThat(response.getTransfers()).hasSize(6);
        assertThat(transfersOf(response, "장보기")).hasSize(4).allMatch(t -> t.getAmount() == 16000);
        assertThat(transfersOf(response, "야식")).hasSize(2).allMatch(t -> t.getAmount() == 6660);
    }

    @Test
    @DisplayName("방 멤버라면 방장이 아니어도 정산 결과를 볼 수 있다")
    void calculate_byMember() {
        pay(gyounghwan, "장보기", 80000, gyounghwan, minwook);

        SettlementResponse response = settlementService.calculate(roomId, minwook.getId());

        assertThat(response.getTransfers()).hasSize(1);
    }

    @Test
    @DisplayName("canSettle: 방장은 모든 줄, 일반 멤버는 자기가 받을 줄만 true")
    void calculate_canSettle() {
        pay(gyounghwan, "장보기", 80000, gyounghwan, minwook, sohyun);   // 경환이 받음
        pay(sohyun, "야식", 20000, sohyun, minwook);                     // 소현이 받음

        SettlementResponse byOwner = settlementService.calculate(roomId, gyounghwan.getId());
        assertThat(byOwner.getTransfers()).allMatch(TransferInfo::isCanSettle);

        SettlementResponse bySohyun = settlementService.calculate(roomId, sohyun.getId());
        assertThat(bySohyun.getTransfers())
                .allMatch(t -> t.isCanSettle() == t.getToUserId().equals(sohyun.getId()));
        assertThat(bySohyun.getNetTransfers())
                .allMatch(n -> n.isCanSettle() == n.getToUserId().equals(sohyun.getId()));

        SettlementResponse byMinwook = settlementService.calculate(roomId, minwook.getId());
        assertThat(byMinwook.getTransfers()).noneMatch(TransferInfo::isCanSettle);   // 보내기만 함
    }

    @Test
    @DisplayName("방 멤버가 아니면 정산 결과를 볼 수 없다")
    void calculate_byOutsider_throwsException() {
        User outsider = userRepository.save(User.builder().loginId("outsider").name("외부인").build());

        assertThatThrownBy(() -> settlementService.calculate(roomId, outsider.getId()))
                .isInstanceOf(NotRoomMemberException.class);
    }

    @Test
    @DisplayName("같은 사람에게 보낼 돈은 결제가 달라도 한 줄로 합쳐진다")
    void calculate_netTransfersMerged() {
        pay(gyounghwan, "장보기", 30000, gyounghwan, minwook, juhwang);   // 10,000씩
        pay(gyounghwan, "택시", 20000, gyounghwan, minwook);              // 10,000

        SettlementResponse response = settlementService.calculate(roomId, gyounghwan.getId());

        assertThat(response.getNetTransfers()).hasSize(2);
        NetTransferInfo minwookNet = response.getNetTransfers().stream()
                .filter(n -> n.getFromUserName().equals("민욱"))
                .findFirst().orElseThrow();
        assertThat(minwookNet.getAmount()).isEqualTo(20000);
        assertThat(minwookNet.getParticipantIds()).hasSize(2);
    }

    @Test
    @DisplayName("방장이 정산 완료한 항목은 다시 계산해도 settled=true로 나온다")
    void calculate_reflectsSettledStatus() {
        pay(gyounghwan, "장보기", 80000, gyounghwan, minwook, juhwang);

        Long participantId = settlementService.calculate(roomId, gyounghwan.getId())
                .getTransfers().get(0).getParticipantId();
        paymentService.markAsSettled(roomId, participantId, gyounghwan.getId());

        SettlementResponse after = settlementService.calculate(roomId, gyounghwan.getId());

        assertThat(after.getTransfers()).filteredOn(TransferInfo::isSettled).hasSize(1);
    }

    @Test
    @DisplayName("절사 단위를 100원으로 바꾸고 다시 계산하면 6,660원이 6,600원이 된다")
    void calculate_changeRoundUnit() {
        pay(sohyun, "야식", 20000, sohyun, gyounghwan, youngjun);
        assertThat(settlementService.calculate(roomId, gyounghwan.getId()).getTransfers())
                .allMatch(t -> t.getAmount() == 6660);

        payRoomService.update(roomId, gyounghwan.getId(),
                PayRoomUpdateRequest.builder().title("Podo MT").roundUnit(100).build());

        assertThat(settlementService.calculate(roomId, gyounghwan.getId()).getTransfers())
                .allMatch(t -> t.getAmount() == 6600);
    }

    @Test
    @DisplayName("공유 문구에 합친 송금 내역이 들어간다")
    void generateShareText() {
        pay(gyounghwan, "장보기", 80000, gyounghwan, minwook, juhwang, youngjun, sohyun);

        String text = settlementService.generateShareText(roomId, gyounghwan.getId());

        assertThat(text)
                .startsWith("'Podo MT' 모임 정산 내용입니다.")
                .contains("민욱님은 경환님에게 16,000원 입금 부탁드립니다.");
    }

    // === 테스트 헬퍼 === //

    /** payer가 직접 결제를 추가 (payer 본인 = 로그인 사용자) */
    private void pay(User payer, String title, int amount, User... participants) {
        paymentService.create(roomId, payer.getId(), PaymentCreateRequest.builder()
                .title(title).amount(amount).payerId(payer.getId())
                .participantIds(Arrays.stream(participants).map(User::getId).toList())
                .build());
    }

    private List<TransferInfo> transfersOf(SettlementResponse response, String paymentTitle) {
        return response.getTransfers().stream()
                .filter(t -> t.getPaymentTitle().equals(paymentTitle))
                .toList();
    }
}