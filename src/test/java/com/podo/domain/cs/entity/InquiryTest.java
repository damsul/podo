package com.podo.domain.cs.entity;

import com.podo.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InquiryTest {

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .loginId("pien")
                .name("최경환")
                .build();
    }

    @Nested
    @DisplayName("Inquiry 생성 테스트")
    class CreateInquiry {

        @Test
        @DisplayName("문의를 생성하면 PENDING 상태로 시작한다")
        void createWithPendingStatus() {
            // when
            Inquiry inquiry = Inquiry.builder()
                    .user(user)
                    .title("정산 계산이 이상해요")
                    .content("80,000원을 5명이 나누면...")
                    .build();

            // then
            assertThat(inquiry.getUser()).isEqualTo(user);
            assertThat(inquiry.getTitle()).isEqualTo("정산 계산이 이상해요");
            assertThat(inquiry.getContent()).isEqualTo("80,000원을 5명이 나누면...");
            assertThat(inquiry.getStatus()).isEqualTo(InquiryStatus.PENDING);
            assertThat(inquiry.getAnswer()).isNull();
            assertThat(inquiry.getAnsweredAt()).isNull();
            assertThat(inquiry.isAnswered()).isFalse();
        }
    }

    @Nested
    @DisplayName("문의 답변 처리 테스트")
    class AnswerInquiry {

        @Test
        @DisplayName("답변하면 status가 ANSWERED로 변경되고 answeredAt이 기록된다")
        void answerInquiry() {
            // given
            Inquiry inquiry = Inquiry.builder()
                    .user(user)
                    .title("질문")
                    .content("내용")
                    .build();
            LocalDateTime beforeAnswer = LocalDateTime.now();

            // when
            inquiry.registerAnswer("답변 내용입니다");

            // then
            assertThat(inquiry.getStatus()).isEqualTo(InquiryStatus.ANSWERED);
            assertThat(inquiry.getAnswer()).isEqualTo("답변 내용입니다");
            assertThat(inquiry.getAnsweredAt()).isNotNull();
            assertThat(inquiry.getAnsweredAt()).isAfterOrEqualTo(beforeAnswer);
            assertThat(inquiry.isAnswered()).isTrue();
        }

        @Test
        @DisplayName("이미 답변된 문의에도 새 답변으로 덮어쓸 수 있다")
        void overwriteAnswer() {
            // given
            Inquiry inquiry = Inquiry.builder()
                    .user(user)
                    .title("질문")
                    .content("내용")
                    .build();
            inquiry.registerAnswer("첫 번째 답변");

            // when
            inquiry.registerAnswer("수정된 답변");

            // then
            assertThat(inquiry.getAnswer()).isEqualTo("수정된 답변");
            assertThat(inquiry.getStatus()).isEqualTo(InquiryStatus.ANSWERED);
        }
    }

    @Nested
    @DisplayName("문의 내용 수정 테스트")
    class UpdateContent {

        @Test
        @DisplayName("PENDING 상태면 문의 내용을 수정할 수 있다")
        void updateContent_whenPending() {
            // given
            Inquiry inquiry = Inquiry.builder()
                    .user(user)
                    .title("원래 제목")
                    .content("원래 내용")
                    .build();

            // when
            inquiry.updateContent("수정된 제목", "수정된 내용");

            // then
            assertThat(inquiry.getTitle()).isEqualTo("수정된 제목");
            assertThat(inquiry.getContent()).isEqualTo("수정된 내용");
        }

        @Test
        @DisplayName("ANSWERED 상태면 문의 내용을 수정할 수 없다")
        void updateContent_whenAnswered() {
            // given
            Inquiry inquiry = Inquiry.builder()
                    .user(user)
                    .title("원래 제목")
                    .content("원래 내용")
                    .build();
            inquiry.registerAnswer("답변 완료");

            // when & then
            assertThatThrownBy(() -> inquiry.updateContent("바꿀 제목", "바꿀 내용"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("답변이 완료된 문의는 수정할 수 없습니다");
        }

        @Test
        @DisplayName("ANSWERED 상태에서 수정 시도해도 원본 데이터는 보존된다")
        void updateContent_whenAnswered_preservesData() {
            // given
            Inquiry inquiry = Inquiry.builder()
                    .user(user)
                    .title("원래 제목")
                    .content("원래 내용")
                    .build();
            inquiry.registerAnswer("답변 완료");

            // when (예외 발생)
            try {
                inquiry.updateContent("바꿀 제목", "바꿀 내용");
            } catch (IllegalStateException ignored) {
                // 예외는 위 테스트에서 검증함
            }

            // then - 원본 데이터 유지
            assertThat(inquiry.getTitle()).isEqualTo("원래 제목");
            assertThat(inquiry.getContent()).isEqualTo("원래 내용");
        }
    }

    @Nested
    @DisplayName("작성자 확인 테스트")
    class WriterCheck {

        @Test
        @DisplayName("작성자 본인이면 isWrittenBy는 true를 반환한다")
        void isWrittenBy_true() {
            // given
            User userWithId = createUserWithId(1L, user);
            Inquiry inquiry = Inquiry.builder()
                    .user(userWithId)
                    .title("질문")
                    .content("내용")
                    .build();

            // when & then
            assertThat(inquiry.isWrittenBy(userWithId)).isTrue();
        }

        @Test
        @DisplayName("작성자가 아니면 isWrittenBy는 false를 반환한다")
        void isWrittenBy_false() {
            // given
            User userWithId = createUserWithId(1L, user);
            User otherUser = createUserWithId(2L, User.builder().name("이민욱").build());
            Inquiry inquiry = Inquiry.builder()
                    .user(userWithId)
                    .title("질문")
                    .content("내용")
                    .build();

            // when & then
            assertThat(inquiry.isWrittenBy(otherUser)).isFalse();
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