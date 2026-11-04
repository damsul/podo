package com.podo.domain.payroom.entity;

import com.podo.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentParticipantTest {

    private User user;
    private Payment payment;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .loginId("pien")
                .name("최경환")
                .build();

        User owner = User.builder()
                .loginId("admin")
                .name("관리자")
                .build();

        PayRoom payRoom = PayRoom.builder()
                .owner(owner)
                .title("Podo MT")
                .build();

        payment = Payment.builder()
                .payRoom(payRoom)
                .payer(user)
                .title("장보기")
                .amount(80000)
                .build();
    }

    @Nested
    @DisplayName("PaymentParticipant 생성 테스트")
    class CreateParticipant {

        @Test
        @DisplayName("결제 참여자를 생성한다")
        void createParticipant() {
            // when
            PaymentParticipant participant = PaymentParticipant.builder()
                    .payment(payment)
                    .user(user)
                    .build();

            // then
            assertThat(participant.getPayment()).isEqualTo(payment);
            assertThat(participant.getUser()).isEqualTo(user);
            assertThat(participant.isSettled()).isFalse();   // 기본값 false
        }
    }

    @Nested
    @DisplayName("정산 상태 변경 테스트")
    class SettlementStatus {

        @Test
        @DisplayName("정산 완료로 표시할 수 있다")
        void markAsSettled() {
            // given
            PaymentParticipant participant = PaymentParticipant.builder()
                    .payment(payment)
                    .user(user)
                    .build();
            assertThat(participant.isSettled()).isFalse();

            // when
            participant.markAsSettled();

            // then
            assertThat(participant.isSettled()).isTrue();
        }

        @Test
        @DisplayName("정산 완료를 다시 미정산으로 되돌릴 수 있다")
        void markAsUnsettled() {
            // given
            PaymentParticipant participant = PaymentParticipant.builder()
                    .payment(payment)
                    .user(user)
                    .build();
            participant.markAsSettled();
            assertThat(participant.isSettled()).isTrue();

            // when
            participant.markAsUnsettled();

            // then
            assertThat(participant.isSettled()).isFalse();
        }

        @Test
        @DisplayName("정산 상태를 여러 번 토글할 수 있다")
        void toggleMultipleTimes() {
            // given
            PaymentParticipant participant = PaymentParticipant.builder()
                    .payment(payment)
                    .user(user)
                    .build();

            // when & then
            participant.markAsSettled();
            assertThat(participant.isSettled()).isTrue();

            participant.markAsUnsettled();
            assertThat(participant.isSettled()).isFalse();

            participant.markAsSettled();
            assertThat(participant.isSettled()).isTrue();
        }
    }

    @Nested
    @DisplayName("isUser 메서드 테스트")
    class IsUserCheck {

        @Test
        @DisplayName("같은 사용자이면 isUser는 true를 반환한다")
        void isUser_true() {
            // given
            User userWithId = createUserWithId(1L, user);
            PaymentParticipant participant = PaymentParticipant.builder()
                    .payment(payment)
                    .user(userWithId)
                    .build();

            // when & then
            assertThat(participant.isUser(userWithId)).isTrue();
        }

        @Test
        @DisplayName("다른 사용자이면 isUser는 false를 반환한다")
        void isUser_false() {
            // given
            User userWithId = createUserWithId(1L, user);
            User otherUser = createUserWithId(2L, User.builder().name("이민욱").build());

            PaymentParticipant participant = PaymentParticipant.builder()
                    .payment(payment)
                    .user(userWithId)
                    .build();

            // when & then
            assertThat(participant.isUser(otherUser)).isFalse();
        }
    }

    // === 테스트 헬퍼 === //

    private User createUserWithId(Long id, User template) {
        try {
            java.lang.reflect.Field idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(template, id);
            return template;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}