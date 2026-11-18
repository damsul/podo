package com.podo.domain.payroom.service;

import com.podo.domain.payroom.dto.response.SettlementResponse;
import com.podo.domain.payroom.dto.response.SettlementResponse.NetTransferInfo;
import com.podo.domain.payroom.dto.response.SettlementResponse.TransferInfo;
import com.podo.domain.payroom.entity.PayRoom;
import com.podo.domain.payroom.entity.Payment;
import com.podo.domain.payroom.entity.PaymentParticipant;
import com.podo.domain.payroom.exception.NotRoomMemberException;
import com.podo.domain.payroom.exception.PayRoomNotFoundException;
import com.podo.domain.payroom.repository.AccountRepository;
import com.podo.domain.payroom.repository.PayRoomRepository;
import com.podo.domain.payroom.repository.PaymentParticipantRepository;
import com.podo.domain.payroom.repository.PaymentRepository;
import com.podo.domain.payroom.repository.RoomMemberRepository;
import com.podo.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.*;

/**
 * SettlementService 단위 테스트 (Mock)
 * - 정산 "계산 규칙"(균등 분할, 결제자 제외, 절사, 같은 사람끼리 합산)을 빠르게 검증한다.
 * - 조회는 결제 목록 1번 + 참여자 묶음 1번(findGroupedByPayment)으로 바뀌었으므로 그 두 개만 흉내 낸다.
 */
@ExtendWith(MockitoExtension.class)
class SettlementServiceTest {

    @Mock private PayRoomRepository payRoomRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private PaymentParticipantRepository participantRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private RoomMemberRepository roomMemberRepository;

    @InjectMocks
    private SettlementService settlementService;

    private static final Long ROOM_ID = 10L;
    private static final Long OWNER_ID = 1L;

    private User gyounghwan;
    private User minwook;
    private User juhwang;
    private User youngjun;
    private User sohyun;
    private PayRoom payRoom;

    private long nextParticipantId = 1L;

    @BeforeEach
    void setUp() {
        gyounghwan = user(1L, "경환");
        minwook = user(2L, "민욱");
        juhwang = user(3L, "주황");
        youngjun = user(4L, "영준");
        sohyun = user(5L, "소현");
        payRoom = room(10);
    }

    @Nested
    @DisplayName("기본 분할")
    class BasicSplit {

        @Test
        @DisplayName("80,000원을 5명이 나누면 결제자를 뺀 4명이 16,000원씩 보낸다")
        void evenSplit() {
            Payment payment = payment(100L, gyounghwan, "장보기", 80000);
            givenSettlement(Map.of(payment, participants(payment, gyounghwan, minwook, juhwang, youngjun, sohyun)));

            SettlementResponse response = settlementService.calculate(ROOM_ID, OWNER_ID);

            assertThat(response.getRoomTitle()).isEqualTo("Podo MT");
            assertThat(response.getTotalAmount()).isEqualTo(80000);
            assertThat(response.getTransfers()).hasSize(4)
                    .allMatch(t -> t.getAmount() == 16000)
                    .allMatch(t -> t.getToUserName().equals("경환"))
                    .noneMatch(t -> t.getFromUserId().equals(gyounghwan.getId()));
        }

        @Test
        @DisplayName("송금 정보에 결제·참여자 정보가 모두 담긴다")
        void transferContainsAllInfo() {
            Payment payment = payment(100L, gyounghwan, "장보기", 80000);
            List<PaymentParticipant> pps = participants(payment, gyounghwan, minwook);
            givenSettlement(Map.of(payment, pps));

            TransferInfo t = settlementService.calculate(ROOM_ID, OWNER_ID).getTransfers().get(0);

            assertThat(t.getFromUserName()).isEqualTo("민욱");
            assertThat(t.getToUserName()).isEqualTo("경환");
            assertThat(t.getAmount()).isEqualTo(40000);
            assertThat(t.getPaymentId()).isEqualTo(100L);
            assertThat(t.getPaymentTitle()).isEqualTo("장보기");
            assertThat(t.getParticipantId()).isEqualTo(pps.get(1).getId());
            assertThat(t.isSettled()).isFalse();
        }

        @Test
        @DisplayName("참여자가 결제자 본인뿐이면 송금 0건 (총액에는 포함)")
        void onlyPayer() {
            Payment payment = payment(100L, gyounghwan, "혼자 야식", 10000);
            givenSettlement(Map.of(payment, participants(payment, gyounghwan)));

            SettlementResponse response = settlementService.calculate(ROOM_ID, OWNER_ID);

            assertThat(response.getTotalAmount()).isEqualTo(10000);
            assertThat(response.getTransfers()).isEmpty();
            assertThat(response.getNetTransfers()).isEmpty();
        }

        @Test
        @DisplayName("결제가 없는 방은 총액 0, 송금 0건")
        void emptyRoom() {
            givenSettlement(Map.of());

            SettlementResponse response = settlementService.calculate(ROOM_ID, OWNER_ID);

            assertThat(response.getTotalAmount()).isZero();
            assertThat(response.getTransfers()).isEmpty();
        }
    }

    @Nested
    @DisplayName("절사")
    class Rounding {

        @ParameterizedTest(name = "절사 {0}원: 20,000 ÷ 3 = 6,666 → {1}원")
        @CsvSource({"1, 6666", "10, 6660", "100, 6600", "1000, 6000"})
        @DisplayName("절사 단위에 따라 1인당 금액이 내림된다 (원래 금액은 rawAmount에 남는다)")
        void roundDown(int roundUnit, int expected) {
            payRoom = room(roundUnit);
            Payment payment = payment(100L, sohyun, "야식", 20000);
            givenSettlement(Map.of(payment, participants(payment, sohyun, gyounghwan, youngjun)));

            SettlementResponse response = settlementService.calculate(ROOM_ID, OWNER_ID);

            assertThat(response.getTransfers()).hasSize(2)
                    .allMatch(t -> t.getAmount() == expected)
                    .allMatch(t -> t.getRawAmount() == 6666);
        }
    }

    @Nested
    @DisplayName("보낼 돈 합치기 (netTransfers)")
    class NetTransfers {

        @Test
        @DisplayName("같은 사람에게 보낼 돈은 결제가 달라도 한 줄로 합쳐진다")
        void sameDirection_isMerged() {
            Payment p1 = payment(100L, gyounghwan, "장보기", 30000);   // 3명 → 10,000
            Payment p2 = payment(101L, gyounghwan, "택시", 20000);     // 2명 → 10,000
            List<PaymentParticipant> pps1 = participants(p1, gyounghwan, minwook, juhwang);
            List<PaymentParticipant> pps2 = participants(p2, gyounghwan, minwook);
            givenSettlement(Map.of(p1, pps1, p2, pps2));

            SettlementResponse response = settlementService.calculate(ROOM_ID, OWNER_ID);

            assertThat(response.getTransfers()).hasSize(3);       // 결제별: 민욱 2건 + 주황 1건
            assertThat(response.getNetTransfers()).hasSize(2);    // 사람별: 민욱→경환, 주황→경환

            NetTransferInfo minwookNet = findNet(response, "민욱");
            assertThat(minwookNet.getAmount()).isEqualTo(20000);
            assertThat(minwookNet.getParticipantIds()).hasSize(2);
        }

        @Test
        @DisplayName("합친 금액은 원래 금액끼리 더한 뒤 한 번만 절사한다")
        void roundAfterSum() {
            payRoom = room(1000);
            Payment p1 = payment(100L, gyounghwan, "커피", 3000);   // 2명 → 1,500
            Payment p2 = payment(101L, gyounghwan, "간식", 3000);   // 2명 → 1,500
            givenSettlement(Map.of(
                    p1, participants(p1, gyounghwan, minwook),
                    p2, participants(p2, gyounghwan, minwook)));

            SettlementResponse response = settlementService.calculate(ROOM_ID, OWNER_ID);

            // 결제별로 절사하면 1,000 + 1,000 = 2,000원이지만, 합친 뒤 절사하면 3,000원
            assertThat(response.getTransfers()).allMatch(t -> t.getAmount() == 1000);
            assertThat(findNet(response, "민욱").getAmount()).isEqualTo(3000);
        }

        @Test
        @DisplayName("묶인 참여자가 모두 정산돼야 합친 줄도 정산 완료가 된다")
        void settledOnlyWhenAllSettled() {
            Payment p1 = payment(100L, gyounghwan, "장보기", 20000);
            Payment p2 = payment(101L, gyounghwan, "택시", 20000);
            List<PaymentParticipant> pps1 = participants(p1, gyounghwan, minwook);
            List<PaymentParticipant> pps2 = participants(p2, gyounghwan, minwook);
            pps1.get(1).markAsSettled();   // 민욱의 장보기만 정산
            givenSettlement(Map.of(p1, pps1, p2, pps2));

            assertThat(findNet(settlementService.calculate(ROOM_ID, OWNER_ID), "민욱").isSettled()).isFalse();

            pps2.get(1).markAsSettled();   // 택시까지 정산
            assertThat(findNet(settlementService.calculate(ROOM_ID, OWNER_ID), "민욱").isSettled()).isTrue();
        }
    }

    @Nested
    @DisplayName("상계 (두 사람 사이 차액만)")
    class Offset {

        @Test
        @DisplayName("서로 보낼 돈이 있으면 차액만 한 줄로 남는다 (16,000 - 6,000 같은 계산)")
        void oppositeDirections_areOffset() {
            Payment p1 = payment(100L, gyounghwan, "장보기", 20000);   // 민욱 → 경환 10,000
            Payment p2 = payment(101L, minwook, "택시", 8000);         // 경환 → 민욱 4,000
            givenSettlement(Map.of(
                    p1, participants(p1, gyounghwan, minwook),
                    p2, participants(p2, minwook, gyounghwan)));

            SettlementResponse response = settlementService.calculate(ROOM_ID, OWNER_ID);

            assertThat(response.getTransfers()).hasSize(2);        // 결제별 내역은 그대로
            assertThat(response.getNetTransfers()).hasSize(1);     // 최종 정산은 한 줄
            NetTransferInfo net = response.getNetTransfers().get(0);
            assertThat(net.getFromUserName()).isEqualTo("민욱");
            assertThat(net.getToUserName()).isEqualTo("경환");
            assertThat(net.getAmount()).isEqualTo(6000);
            assertThat(net.getParticipantIds()).hasSize(2);        // 양방향 참여자 모두 포함
            assertThat(net.isZeroBalance()).isFalse();
        }

        @Test
        @DisplayName("보낼 돈이 같으면 '주고받을 돈 없음' 줄이 되고 체크할 수 없다")
        void sameAmount_isZeroBalance() {
            Payment p1 = payment(100L, gyounghwan, "커피", 10000);   // 민욱 → 경환 5,000
            Payment p2 = payment(101L, minwook, "간식", 10000);      // 경환 → 민욱 5,000
            givenSettlement(Map.of(
                    p1, participants(p1, gyounghwan, minwook),
                    p2, participants(p2, minwook, gyounghwan)));

            NetTransferInfo net = settlementService.calculate(ROOM_ID, OWNER_ID).getNetTransfers().get(0);

            assertThat(net.isZeroBalance()).isTrue();
            assertThat(net.getAmount()).isZero();
            assertThat(net.isCanSettle()).isFalse();   // 방장이어도 버튼 없음
        }

        @Test
        @DisplayName("상계는 원래 금액끼리 빼고 마지막에 한 번만 절사한다")
        void offsetBeforeRounding() {
            payRoom = room(1000);
            Payment p1 = payment(100L, gyounghwan, "장보기", 10000);   // 민욱 → 경환 5,000
            Payment p2 = payment(101L, minwook, "커피", 3000);         // 경환 → 민욱 1,500
            givenSettlement(Map.of(
                    p1, participants(p1, gyounghwan, minwook),
                    p2, participants(p2, minwook, gyounghwan)));

            NetTransferInfo net = settlementService.calculate(ROOM_ID, OWNER_ID).getNetTransfers().get(0);

            // 결제별로 절사하면 5,000 - 1,000 = 4,000원이지만, 원래 금액 3,500원을 절사하면 3,000원
            assertThat(net.getRawAmount()).isEqualTo(3500);
            assertThat(net.getAmount()).isEqualTo(3000);
        }

        @Test
        @DisplayName("방장이 아니면 상계 후 '최종 받는 사람'만 그 줄을 체크할 수 있다")
        void canSettle_onlyNetReceiver() {
            Payment p1 = payment(100L, juhwang, "숙소", 20000);   // 민욱 → 주황 10,000
            Payment p2 = payment(101L, minwook, "택시", 4000);    // 주황 → 민욱 2,000
            Map<Payment, List<PaymentParticipant>> data = Map.of(
                    p1, participants(p1, juhwang, minwook),
                    p2, participants(p2, minwook, juhwang));
            givenSettlement(data, juhwang.getId(), minwook.getId());

            NetTransferInfo byReceiver = settlementService.calculate(ROOM_ID, juhwang.getId()).getNetTransfers().get(0);
            NetTransferInfo bySender = settlementService.calculate(ROOM_ID, minwook.getId()).getNetTransfers().get(0);

            assertThat(byReceiver.getToUserName()).isEqualTo("주황");
            assertThat(byReceiver.getAmount()).isEqualTo(8000);
            assertThat(byReceiver.isCanSettle()).isTrue();
            assertThat(bySender.isCanSettle()).isFalse();
        }

        @Test
        @DisplayName("공유 문구에서 '주고받을 돈 없음' 줄은 빠진다")
        void shareText_skipsZeroBalance() {
            Payment p1 = payment(100L, gyounghwan, "커피", 10000);
            Payment p2 = payment(101L, minwook, "간식", 10000);
            Payment p3 = payment(102L, gyounghwan, "택시", 20000);   // 주황 → 경환 10,000
            givenSettlement(Map.of(
                    p1, participants(p1, gyounghwan, minwook),
                    p2, participants(p2, minwook, gyounghwan),
                    p3, participants(p3, gyounghwan, juhwang)));
            SettlementResponse settlement = settlementService.calculate(ROOM_ID, OWNER_ID);

            String text = settlementService.buildShareText(settlement, List.of());

            assertThat(text)
                    .contains("주황님은 경환님에게 10,000원 입금 부탁드립니다.")
                    .doesNotContain("민욱님은")
                    .doesNotContain("경환님은");
        }
    }

    @Nested
    @DisplayName("권한·예외")
    class Exceptions {

        @Test
        @DisplayName("방 멤버가 아니면 정산을 볼 수 없고, 결제도 조회하지 않는다")
        void notMember() {
            given(payRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(payRoom));
            given(roomMemberRepository.existsMember(ROOM_ID, 99L)).willReturn(false);

            assertThatThrownBy(() -> settlementService.calculate(ROOM_ID, 99L))
                    .isInstanceOf(NotRoomMemberException.class);

            verify(paymentRepository, never()).findAllByPayRoomWithPayer(any());
        }

        @Test
        @DisplayName("없는 방이면 PayRoomNotFoundException")
        void roomNotFound() {
            given(payRoomRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> settlementService.calculate(999L, OWNER_ID))
                    .isInstanceOf(PayRoomNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("공유 문구")
    class ShareText {

        @Test
        @DisplayName("합친 송금 내역으로 문구를 만들고, 계좌가 없으면 계좌정보 칸은 빠진다")
        void buildShareText_withoutAccounts() {
            Payment payment = payment(100L, gyounghwan, "장보기", 80000);
            givenSettlement(Map.of(payment, participants(payment, gyounghwan, minwook, juhwang, youngjun, sohyun)));
            SettlementResponse settlement = settlementService.calculate(ROOM_ID, OWNER_ID);

            String text = settlementService.buildShareText(settlement, List.of());

            assertThat(text)
                    .startsWith("'Podo MT' 모임 정산 내용입니다.")
                    .contains("민욱님은 경환님에게 16,000원 입금 부탁드립니다.")
                    .doesNotContain("[계좌정보]");
        }
    }

    // === 테스트 헬퍼 === //

    /** 방 조회 + 멤버 확인 + 결제 목록 + 참여자 묶음을 한 번에 준비 (방장이 조회) */
    private void givenSettlement(Map<Payment, List<PaymentParticipant>> data) {
        givenSettlement(data, OWNER_ID);
    }

    /** 조회하는 사용자를 지정해서 준비 (각 사용자를 방 멤버로 응답) */
    private void givenSettlement(Map<Payment, List<PaymentParticipant>> data, Long... viewerIds) {
        List<Payment> payments = data.keySet().stream()
                .sorted((a, b) -> a.getId().compareTo(b.getId()))
                .toList();
        Map<Long, List<PaymentParticipant>> grouped = new java.util.HashMap<>();
        data.forEach((p, pps) -> grouped.put(p.getId(), pps));

        given(payRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(payRoom));
        for (Long viewerId : viewerIds) {
            given(roomMemberRepository.existsMember(ROOM_ID, viewerId)).willReturn(true);
        }
        given(paymentRepository.findAllByPayRoomWithPayer(payRoom)).willReturn(payments);
        given(participantRepository.findGroupedByPayment(anyList())).willReturn(grouped);
    }

    private NetTransferInfo findNet(SettlementResponse response, String fromName) {
        return response.getNetTransfers().stream()
                .filter(n -> n.getFromUserName().equals(fromName))
                .findFirst()
                .orElseThrow();
    }

    private PayRoom room(int roundUnit) {
        return setId(PayRoom.class, PayRoom.builder()
                .owner(gyounghwan).title("Podo MT").roundUnit(roundUnit).build(), ROOM_ID);
    }

    private User user(Long id, String name) {
        return setId(User.class, User.builder().loginId("user" + id).name(name).build(), id);
    }

    private Payment payment(Long id, User payer, String title, int amount) {
        return setId(Payment.class, Payment.builder()
                .payRoom(payRoom).payer(payer).title(title).amount(amount).build(), id);
    }

    private List<PaymentParticipant> participants(Payment payment, User... users) {
        return java.util.Arrays.stream(users)
                .map(u -> setId(PaymentParticipant.class,
                        PaymentParticipant.builder().payment(payment).user(u).build(), nextParticipantId++))
                .toList();
    }

    private static <T> T setId(Class<T> clazz, T entity, Long id) {
        try {
            Field idField = clazz.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(entity, id);
            return entity;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}