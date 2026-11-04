package com.podo.domain.payroom.entity;

import com.podo.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Field;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentTest {

    private User payer;
    private PayRoom payRoom;

    @BeforeEach
    void setUp() {
        payer = User.builder()
                .loginId("pien")
                .name("최경환")
                .build();

        User owner = User.builder()
                .loginId("admin")
                .name("관리자")
                .build();

        payRoom = PayRoom.builder()
                .owner(owner)
                .title("Podo MT")
                .build();
    }

    @Nested
    @DisplayName("Payment 생성 테스트")
    class CreatePayment {

        @Test
        @DisplayName("결제 항목을 생성한다")
        void createPayment() {
            LocalDate paidAt = LocalDate.of(2026, 5, 1);

            Payment payment = Payment.builder()
                    .payRoom(payRoom)
                    .payer(payer)
                    .title("장보기")
                    .amount(80000)
                    .paidAt(paidAt)
                    .build();

            assertThat(payment.getPayRoom()).isEqualTo(payRoom);
            assertThat(payment.getPayer()).isEqualTo(payer);
            assertThat(payment.getTitle()).isEqualTo("장보기");
            assertThat(payment.getAmount()).isEqualTo(80000);
            assertThat(payment.getPaidAt()).isEqualTo(paidAt);
        }

        @Test
        @DisplayName("paidAt이 null이면 오늘 날짜로 자동 설정된다")
        void paidAtDefaultsToToday() {
            Payment payment = Payment.builder()
                    .payRoom(payRoom)
                    .payer(payer)
                    .title("야식 배달")
                    .amount(20000)
                    .build();

            assertThat(payment.getPaidAt()).isEqualTo(LocalDate.now());
        }
    }

    @Nested
    @DisplayName("Payment 금액 검증 테스트")
    class AmountValidation {

        @ParameterizedTest
        @DisplayName("amount가 0 이하이면 예외가 발생한다")
        @ValueSource(ints = {0, -1, -1000, -99999})
        void createWithInvalidAmount(int invalidAmount) {
            assertThatThrownBy(() -> Payment.builder()
                    .payRoom(payRoom)
                    .payer(payer)
                    .title("장보기")
                    .amount(invalidAmount)
                    .build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("결제 금액은 0보다 커야 합니다.");
        }

        @ParameterizedTest
        @DisplayName("amount가 양수이면 정상 생성된다")
        @ValueSource(ints = {1, 100, 80000, Integer.MAX_VALUE})
        void createWithValidAmount(int validAmount) {
            Payment payment = Payment.builder()
                    .payRoom(payRoom)
                    .payer(payer)
                    .title("장보기")
                    .amount(validAmount)
                    .build();

            assertThat(payment.getAmount()).isEqualTo(validAmount);
        }

        @Test
        @DisplayName("update 시에도 amount 검증이 적용된다")
        void updateWithInvalidAmount() {
            Payment payment = Payment.builder()
                    .payRoom(payRoom)
                    .payer(payer)
                    .title("장보기")
                    .amount(80000)
                    .build();

            assertThatThrownBy(() -> payment.update("장보기 수정", 0, LocalDate.now()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("결제 금액은 0보다 커야 합니다");
        }
    }

    @Nested
    @DisplayName("Payment 수정 테스트")
    class UpdatePayment {

        @Test
        @DisplayName("결제 항목을 수정할 수 있다")
        void updatePayment() {
            Payment payment = Payment.builder()
                    .payRoom(payRoom)
                    .payer(payer)
                    .title("장보기")
                    .amount(80000)
                    .paidAt(LocalDate.of(2026, 5, 1))
                    .build();

            payment.update("장보기 (수정)", 90000, LocalDate.of(2026, 5, 2));

            assertThat(payment.getTitle()).isEqualTo("장보기 (수정)");
            assertThat(payment.getAmount()).isEqualTo(90000);
            assertThat(payment.getPaidAt()).isEqualTo(LocalDate.of(2026, 5, 2));
        }
    }

    @Nested
    @DisplayName("Payment 결제자 확인 테스트")
    class PayerCheck {

        @Test
        @DisplayName("결제자 본인이면 isPayer는 true를 반환한다")
        void isPaidBy_true() {
            User payerWithId = createUserWithId(1L, payer);
            Payment payment = Payment.builder()
                    .payRoom(payRoom)
                    .payer(payerWithId)
                    .title("장보기")
                    .amount(80000)
                    .build();

            assertThat(payment.isPaidBy(payerWithId)).isTrue();
        }

        @Test
        @DisplayName("결제자가 아니면 isPayer는 false를 반환한다")
        void isPaidBy_false() {
            User payerWithId = createUserWithId(1L, payer);
            User otherUser = createUserWithId(2L, User.builder().name("이민욱").build());

            Payment payment = Payment.builder()
                    .payRoom(payRoom)
                    .payer(payerWithId)
                    .title("장보기")
                    .amount(80000)
                    .build();

            assertThat(payment.isPaidBy(otherUser)).isFalse();
        }

        private User createUserWithId(long id, User template) {
            try {
                Field idField = User.class.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(template, id);
                return template;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

}