package com.podo.domain.payroom.repository;

import com.podo.domain.payroom.entity.PayRoom;
import com.podo.domain.payroom.entity.RoomStatus;
import com.podo.domain.user.entity.User;
import com.podo.domain.user.repository.UserRepository;
import com.podo.global.config.JpaAuditingConfig;
import jakarta.persistence.EntityManager;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;


@DataJpaTest
@ActiveProfiles("test")
@Import(JpaAuditingConfig.class)
class PayRoomRepositoryTest {

    @Autowired
    private PayRoomRepository payRoomRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager em;

    private User owner;
    private User otherUser;

    @BeforeEach
    void setUp() {
        owner = userRepository.save(User.builder()
                .loginId("pien")
                .name("최경환")
                .email("pien@naver.com")
                .build());

        otherUser = userRepository.save(User.builder()
                .loginId("minwook")
                .name("이민욱")
                .email("minwook@daum.net")
                .build());
    }

    @Nested
    @DisplayName("기본 CRUD 테스트")
    class BasicCrud {

        @Test
        @DisplayName("PayRoom을 저장하면 ID가 생성된다")
        void savePayRoom() {
            PayRoom room = PayRoom.builder()
                    .owner(owner)
                    .title("Podo MT")
                    .dueDate(LocalDate.of(2026, 6, 12))
                    .roundUnit(10)
                    .build();

            PayRoom saved = payRoomRepository.save(room);

            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getTitle()).isEqualTo("Podo MT");
            assertThat(saved.getStatus()).isEqualTo(RoomStatus.IN_PROGRESS);
            assertThat(saved.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("ID로 PayRoom을 조회할 수 있다")
        void findById() {
            PayRoom saved = payRoomRepository.save(PayRoom.builder()
                    .owner(owner)
                    .title("Podo MT")
                    .build());

            Optional<PayRoom> found = payRoomRepository.findById(saved.getId());

            assertThat(found).isPresent();
            assertThat(found.get().getTitle()).isEqualTo("Podo MT");
        }
    }

    @Nested
    @DisplayName("연관관계 매핑 테스트")
    class Associations {

        @Test
        @DisplayName("RayRoom 저장 시 owner와의 연관관계가 정상 매핑된다")
        void ownerAssociation() {
            PayRoom room = PayRoom.builder()
                    .owner(owner)
                    .title("Podo MT")
                    .build();

            PayRoom saved = payRoomRepository.save(room);

            em.flush();
            em.clear();

            PayRoom reloaded = payRoomRepository.findById(saved.getId()).orElseThrow();

            assertThat(reloaded.getOwner()).isNotNull();
            assertThat(reloaded.getOwner().getId()).isEqualTo(owner.getId());
            assertThat(reloaded.getOwner().getName()).isEqualTo(owner.getName());
        }

        @Test
        @DisplayName("owner는 LAZY 로딩이라 처음 접근 시점에 쿼리가 실행된다")
        void lazyLoading() {
            PayRoom saved = payRoomRepository.save(PayRoom.builder()
                    .owner(owner)
                    .title("Podo MT")
                    .build());
            em.flush();
            em.clear();

            PayRoom reloaded = payRoomRepository.findById(saved.getId()).orElseThrow();

            // 이 시점엔 owner를 안 가져옴(프록시)
            // reloaded.getOwner()를 호출해도 ID까지는 프록시로 알 수 있음
            assertThat(reloaded.getOwner().getId()).isEqualTo(owner.getId());

            // 실제 필드 접근 시점에 쿼리 실행 (LAZY)
            assertThat(reloaded.getOwner().getName()).isEqualTo("최경환");
        }
    }

    @Nested
    @DisplayName("커스텀 쿼리 메서드 테스트")
    class CustomQueries {

        @Test
        @DisplayName("owner로 PayRoom 목록을 조회할 수 있다")
        void findByOwner() {
            payRoomRepository.save(PayRoom.builder()
                    .owner(owner)
                    .title("Podo MT")
                    .build());

            payRoomRepository.save(PayRoom.builder()
                    .owner(owner)
                    .title("넷플릭스 결제")
                    .build());

            payRoomRepository.save(PayRoom.builder()
                    .owner(otherUser)
                    .title("다른 사람 방")
                    .build());

            List<PayRoom> myRooms = payRoomRepository.findByOwner(owner);

            assertThat(myRooms).hasSize(2);
            assertThat(myRooms).extracting(PayRoom::getTitle)
                    .containsExactlyInAnyOrder("Podo MT", "넷플릭스 결제");
        }

        @Test
        @DisplayName("onwer와 status로 PayRoom 목록을 조회할 수 있따")
        void findByOwnerAndStatus() {
            PayRoom inProgress = payRoomRepository.save(PayRoom.builder()
                    .owner(owner)
                    .title("진행중인 방")
                    .build());

            PayRoom completed = payRoomRepository.save(PayRoom.builder()
                    .owner(owner)
                    .title("완료된 방")
                    .build());
            completed.complete();
            payRoomRepository.save(completed);

            List<PayRoom> inProgressRooms = payRoomRepository.findByOwnerAndStatus(owner, RoomStatus.IN_PROGRESS);
            List<PayRoom> completedRooms = payRoomRepository.findByOwnerAndStatus(owner, RoomStatus.COMPLETED);

            assertThat(inProgressRooms).hasSize(1);
            assertThat(inProgressRooms.get(0).getTitle()).isEqualTo("진행중인 방");

            assertThat(completedRooms).hasSize(1);
            assertThat(completedRooms.get(0).getTitle()).isEqualTo("완료된 방");
        }

        @Test
        @DisplayName("조회 결화가 없으면 빈 리스트를 반환한다")
        void findByOwner_empty() {
            List<PayRoom> rooms = payRoomRepository.findByOwner(owner);

            assertThat(rooms).isEmpty();
        }
    }

    @Nested
    @DisplayName("상태 변경 영속화 테스트")
    class StatusPersistence {

        @Test
        @DisplayName("PayRoom 상태 변경이 DB에 반영된다")
        void statusChange() {
            PayRoom saved = payRoomRepository.save(PayRoom.builder()
                    .owner(owner)
                    .title("Podo MT")
                    .build());
            assertThat(saved.getStatus()).isEqualTo(RoomStatus.IN_PROGRESS);

            saved.complete();
            em.flush();
            em.clear();

            PayRoom reloaded = payRoomRepository.findById(saved.getId()).orElseThrow();
            assertThat(reloaded.getStatus()).isEqualTo(RoomStatus.COMPLETED);

        }
    }
}