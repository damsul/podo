package com.podo.domain.payroom.entity;

import com.podo.domain.user.entity.User;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

class PayRoomTest {

    private User owner;
    private User otherUser;

    @BeforeEach
    void setUp() {
        owner = User.builder()
                .loginId("pien")
                .name("최경환")
                .build();

        otherUser = User.builder()
                .loginId("minwook")
                .name("이민욱")
                .build();
    }

    @Nested
    @DisplayName("PayRoom 생성 테스트")
    class CreatePayRoom {
        @Test
        @DisplayName("기본값으로 PayRoom을 생성한다")
        void createWithDefaults() {
            PayRoom room = PayRoom.builder()
                    .owner(owner)
                    .title("Podo MT")
                    .build();

            assertThat(room.getOwner()).isEqualTo(owner);
            assertThat(room.getTitle()).isEqualTo("Podo MT");
            assertThat(room.getStatus()).isEqualTo(RoomStatus.IN_PROGRESS);
            assertThat(room.getRoundUnit()).isEqualTo(10);
            assertThat(room.getDueDate()).isNull();
        }

        @Test
        @DisplayName("모든 필드를 지정해서 Room을 생성한다")
        void createWithAllFields() {
            LocalDate dueDate = LocalDate.of(2026, 6, 12);

            PayRoom room = PayRoom.builder()
                    .owner(owner)
                    .title("Podo MT")
                    .dueDate(dueDate)
                    .roundUnit(100)
                    .build();

            assertThat(room.getDueDate()).isEqualTo(dueDate);
            assertThat(room.getRoundUnit()).isEqualTo(100);
        }

        @Test
        @DisplayName("roundUnit이 1, 10, 100, 1000원 외의 값이면 예외가 발생한다")
        void createWithInvalidRoundUnit() {
            assertThatThrownBy(() -> PayRoom.builder()
                    .owner(owner)
                    .title("Podo MT")
                    .roundUnit(50)  // ← 잘못된 값
                    .build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("절사 단위는 1, 10, 100, 1000원만 가능합니다");
        }
    }

    @Nested
    @DisplayName("PayRoom 상태 전이 테스트")
    class StatusTransition {

        @Test
        @DisplayName("진행중인 방을 완료 상태로 변경할 수 있다")
        void complete() {
            // given
            PayRoom room = PayRoom.builder()
                    .owner(owner)
                    .title("Podo MT")
                    .build();
            assertThat(room.getStatus()).isEqualTo(RoomStatus.IN_PROGRESS);

            // when
            room.complete();

            // then
            assertThat(room.getStatus()).isEqualTo(RoomStatus.COMPLETED);
        }

        @Test
        @DisplayName("완료된 방을 다시 진행중으로 되돌릴 수 있다")
        void reopen() {
            // given
            PayRoom room = PayRoom.builder()
                    .owner(owner)
                    .title("Podo MT")
                    .build();
            room.complete();

            // when
            room.reopen();

            // then
            assertThat(room.getStatus()).isEqualTo(RoomStatus.IN_PROGRESS);
        }
    }

    @Nested
    @DisplayName("PayRoom 정보 수정 테스트")
    class UpdateInfo {

        @Test
        @DisplayName("방 정보를 수정할 수 있다")
        void updateInfo() {
            // given
            PayRoom room = PayRoom.builder()
                    .owner(owner)
                    .title("Podo MT")
                    .build();

            // when
            room.updateInfo("Podo 여름 MT", LocalDate.of(2026, 7, 1), 100);

            // then
            assertThat(room.getTitle()).isEqualTo("Podo 여름 MT");
            assertThat(room.getDueDate()).isEqualTo(LocalDate.of(2026, 7, 1));
            assertThat(room.getRoundUnit()).isEqualTo(100);
        }

        @Test
        @DisplayName("방 정보 수정 시 roundUnit이 null이면 기존 값을 유지한다")
        void updateInfoWithNullRoundUnit() {
            // given
            PayRoom room = PayRoom.builder()
                    .owner(owner)
                    .title("Podo MT")
                    .roundUnit(100)
                    .build();

            // when
            room.updateInfo("새 제목", null, null);

            // then
            assertThat(room.getRoundUnit()).isEqualTo(100);  // 기존 값 유지
        }

        @Test
        @DisplayName("방 정보 수정 시 잘못된 roundUnit이면 예외가 발생한다")
        void updateInfoWithInvalidRoundUnit() {
            // given
            PayRoom room = PayRoom.builder()
                    .owner(owner)
                    .title("Podo MT")
                    .build();

            // when & then
            assertThatThrownBy(() -> room.updateInfo("새 제목", null, 77))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("절사 단위");
        }
    }

    @Nested
    @DisplayName("PayRoom 권한 체크 테스트")
    class OwnerCheck {

        @Test
        @DisplayName("방장 본인이면 isOwner는 true를 반환한다")
        void isOwner_true() {
            User ownerWithId = createUserWithId(1L, owner);
            PayRoom room = PayRoom.builder()
                    .owner(ownerWithId)
                    .title("Podo MT")
                    .build();

            assertThat(room.isOwner(ownerWithId)).isTrue();
        }

        @Test
        @DisplayName("방장이 아니면 isOwner는 false를 반환한다")
        void isOwner_false() {
            User ownerWithId = createUserWithId(1L, owner);
            User otherWithId = createUserWithId(2L, otherUser);
            PayRoom room = PayRoom.builder()
                    .owner(ownerWithId)
                    .title("Podo MT")
                    .build();

            assertThat(room.isOwner(otherWithId)).isFalse();
        }


        /**
         * 테스트용 User에 ID를 강제로 주입하는 헬퍼.
         * 엔티티는 보통 DB persist 후 ID가 생성되지만,
         * 단위 테스트에서는 ID 비교가 필요할 때 reflection으로 주입한다.
         */
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