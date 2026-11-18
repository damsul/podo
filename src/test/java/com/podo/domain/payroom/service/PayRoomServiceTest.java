package com.podo.domain.payroom.service;

import com.podo.domain.payroom.dto.request.PayRoomCreateRequest;
import com.podo.domain.payroom.dto.response.PayRoomResponse;
import com.podo.domain.payroom.dto.request.PayRoomUpdateRequest;
import com.podo.domain.payroom.entity.PayRoom;
import com.podo.domain.payroom.entity.RoomMember;
import com.podo.domain.payroom.entity.RoomStatus;
import com.podo.domain.payroom.exception.AlreadyRoomMemberException;
import com.podo.domain.payroom.exception.NotRoomMemberException;
import com.podo.domain.payroom.exception.NotRoomOwnerException;
import com.podo.domain.payroom.exception.PayRoomNotFoundException;
import com.podo.domain.payroom.repository.AccountRepository;
import com.podo.domain.payroom.repository.PayRoomRepository;
import com.podo.domain.payroom.repository.PaymentRepository;
import com.podo.domain.payroom.repository.RoomMemberRepository;
import com.podo.domain.user.entity.User;
import com.podo.domain.user.exception.UserNotFoundException;
import com.podo.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class PayRoomServiceTest {

    @Mock
    private PayRoomRepository payRoomRepository;

    @Mock
    private RoomMemberRepository roomMemberRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PaymentRepository paymentRepository;   // 관리자 상세용 (이 테스트에선 사용 안 함)

    @Mock
    private AccountRepository accountRepository;   // 관리자 상세용 (이 테스트에선 사용 안 함)

    @InjectMocks
    private PayRoomService payRoomService;

    private User owner;
    private User otherUser;
    private PayRoom payRoom;

    @BeforeEach
    void setUp() {
        owner = createUserWithId(1L, User.builder()
                .loginId("pien")
                .name("최경환")
                .build());

        otherUser = createUserWithId(2L, User.builder()
                .loginId("minwook")
                .name("이민욱")
                .build());

        payRoom = createPayRoomWithId(10L, PayRoom.builder()
                .owner(owner)
                .title("Podo MT")
                .build());
    }

    @Nested
    @DisplayName("방 생성 테스트")
    class CreateRoom {

        @Test
        @DisplayName("방을 생성하면 방장이 자동으로 RoomMember에 추가된다")
        void create_addOwnerAsMember() {
            PayRoomCreateRequest request = PayRoomCreateRequest.builder()
                    .title("Podo MT")
                    .dueDate(LocalDate.of(2026, 6, 12))
                    .roundUnit(10)
                    .build();

            given(userRepository.findById(1L)).willReturn(Optional.of(owner));
            given(payRoomRepository.save(any(PayRoom.class))).willReturn(payRoom);

            PayRoomResponse response = payRoomService.create(1L, request);

            assertThat(response.getTitle()).isEqualTo("Podo MT");
            assertThat(response.getOwnerName()).isEqualTo("최경환");

            verify(roomMemberRepository).save(any(RoomMember.class));
        }

        @Test
        @DisplayName("존재하지 않는 사용자가 방을 만들려고 하면 UserNotFoundException")
        void create_userNotFound() {
            PayRoomCreateRequest request = PayRoomCreateRequest.builder()
                    .title("Podo MT")
                    .build();

            given(userRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> payRoomService.create(999L, request))
                    .isInstanceOf(UserNotFoundException.class);

            verify(payRoomRepository, never()).save(any(PayRoom.class));
            verify(roomMemberRepository, never()).save(any(RoomMember.class));
        }
    }

    @Nested
    @DisplayName("방 조회 테스트")
    class FindRoom {

        @Test
        @DisplayName("방 멤버는 ID로 방을 조회할 수 있다")
        void findById_success() {
            given(payRoomRepository.findById(10L)).willReturn(Optional.of(payRoom));
            given(userRepository.findById(1L)).willReturn(Optional.of(owner));
            given(roomMemberRepository.existsByPayRoomAndUser(payRoom, owner)).willReturn(true);

            PayRoomResponse response = payRoomService.findById(10L, 1L);

            assertThat(response.getId()).isEqualTo(10L);
            assertThat(response.getTitle()).isEqualTo("Podo MT");
        }

        @Test
        @DisplayName("존재하지 않는 방 ID로 조회 시 PayRoomNotFoundException")
        void findById_notFound() {
            given(payRoomRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> payRoomService.findById(999L, 1L))
                    .isInstanceOf(PayRoomNotFoundException.class)
                    .hasMessageContaining("999");
        }

        @Test
        @DisplayName("방 멤버가 아니면 방을 조회할 수 없다 (NotRoomMemberException)")
        void findById_byNonMember_throwsException() {
            given(payRoomRepository.findById(10L)).willReturn(Optional.of(payRoom));
            given(userRepository.findById(2L)).willReturn(Optional.of(otherUser));
            given(roomMemberRepository.existsByPayRoomAndUser(payRoom, otherUser)).willReturn(false);

            assertThatThrownBy(() -> payRoomService.findById(10L, 2L))
                    .isInstanceOf(NotRoomMemberException.class);
        }

        @Test
        @DisplayName("방 멤버가 아니면 멤버 목록도 볼 수 없다")
        void findMembers_byNonMember_throwsException() {
            given(payRoomRepository.findById(10L)).willReturn(Optional.of(payRoom));
            given(userRepository.findById(2L)).willReturn(Optional.of(otherUser));
            given(roomMemberRepository.existsByPayRoomAndUser(payRoom, otherUser)).willReturn(false);

            assertThatThrownBy(() -> payRoomService.findMembers(10L, 2L))
                    .isInstanceOf(NotRoomMemberException.class);

            verify(roomMemberRepository, never()).findByPayRoom(any());
        }

        @Test
        @DisplayName("내 방 목록을 조회한다 (최신순)")
        void findByRooms_success() {
            PayRoom room1 = createPayRoomWithId(10L, PayRoom.builder().owner(owner).title("방1").build());
            PayRoom room2 = createPayRoomWithId(11L, PayRoom.builder().owner(owner).title("방2").build());

            setCreatedAt(room1, LocalDateTime.of(2026, 5, 1, 10,0));
            setCreatedAt(room2, LocalDateTime.of(2026, 5, 2, 10,0));

            RoomMember member1 = RoomMember.builder().payRoom(room1).user(owner).build();
            RoomMember member2 = RoomMember.builder().payRoom(room2).user(owner).build();

            given(userRepository.findById(1L)).willReturn(Optional.of(owner));
            given(roomMemberRepository.findByUser(owner)).willReturn(List.of(member1, member2));

            List<PayRoomResponse> responses = payRoomService.findMyRooms(1L);

            assertThat(responses).hasSize(2);
            assertThat(responses).extracting(PayRoomResponse::getTitle)
                    .containsExactly("방2", "방1");
        }

        @Test
        @DisplayName("상태별로 내 방 목록을 조회한다")
        void findByRoomsByStatus_success() {
            given(userRepository.findById(1L)).willReturn(Optional.of(owner));
            given(payRoomRepository.findByOwnerAndStatus(owner, RoomStatus.IN_PROGRESS))
                    .willReturn(List.of(payRoom));

            List<PayRoomResponse> responses = payRoomService.findMyRoomsByStatus(1L, RoomStatus.IN_PROGRESS);

            assertThat(responses).hasSize(1);
        }
    }



    @Nested
    @DisplayName("방 수정 테스트")
    class UpdateRoom {

        @Test
        @DisplayName("방장은 방 정보를 수정할 수 있다")
        void update_byOwner_success() {
            PayRoomUpdateRequest request = PayRoomUpdateRequest.builder()
                    .title("Podo 여름 MT")
                    .dueDate(LocalDate.of(2026, 7, 1))
                    .roundUnit(100)
                    .build();

            given(payRoomRepository.findById(10L)).willReturn(Optional.of(payRoom));
            given(userRepository.findById(1L)).willReturn(Optional.of(owner));

            PayRoomResponse response = payRoomService.update(10L, 1L, request);

            assertThat(response.getTitle()).isEqualTo("Podo 여름 MT");
            assertThat(response.getRoundUnit()).isEqualTo(100);
        }

        @Test
        @DisplayName("방장이 아닌 사용자가 수정하려 하면 NotRoomOwnerException")
        void update_byNonOwner_throwsException() {
            PayRoomUpdateRequest request = PayRoomUpdateRequest.builder()
                    .title("바꿀 제목")
                    .build();

            given(payRoomRepository.findById(10L)).willReturn(Optional.of(payRoom));
            given(userRepository.findById(2L)).willReturn(Optional.of(otherUser));

            assertThatThrownBy(() -> payRoomService.update(10L, 2L, request))
                    .isInstanceOf(NotRoomOwnerException.class);
        }
    }

    @Nested
    @DisplayName("방 상태 변경 테스트")
    class StatusChange {
        @Test
        @DisplayName("방장은 방을 완료 상태로 만들 수 있다")
        void complete_byOwner_success() {
            given(payRoomRepository.findById(10L)).willReturn(Optional.of(payRoom));
            given(userRepository.findById(1L)).willReturn(Optional.of(owner));

            PayRoomResponse response = payRoomService.complete(10L, 1L);

            assertThat(response.getStatus()).isEqualTo(RoomStatus.COMPLETED);
        }

        @Test
        @DisplayName("방장이 아니면 방을 완료할 수 없다")
        void complete_byNonOwner_throwsException() {
            given(payRoomRepository.findById(10L)).willReturn(Optional.of(payRoom));
            given(userRepository.findById(2L)).willReturn(Optional.of(otherUser));

            assertThatThrownBy(() -> payRoomService.complete(10L, 2L))
                    .isInstanceOf(NotRoomOwnerException.class);
        }

        @Test
        @DisplayName("방장은 완료된 방을 다시 진행중으로 되돌릴 수 있다")
        void reopen_byOwner_success() {
            payRoom.complete();
            given(payRoomRepository.findById(10L)).willReturn(Optional.of(payRoom));
            given(userRepository.findById(1L)).willReturn(Optional.of(owner));

            PayRoomResponse response = payRoomService.reopen(10L, 1L);

            assertThat(response.getStatus()).isEqualTo(RoomStatus.IN_PROGRESS);
        }
    }

    @Nested
    @DisplayName("절사 단위 변경 테스트 (정산 화면)")
    class ChangeRoundUnit {

        @Test
        @DisplayName("방장은 절사 단위만 바꿀 수 있고, 제목·마감일은 그대로다")
        void changeRoundUnit_byOwner_success() {
            PayRoom room = createPayRoomWithId(10L, PayRoom.builder()
                    .owner(owner).title("Podo MT").dueDate(LocalDate.of(2026, 6, 12)).roundUnit(100).build());
            given(payRoomRepository.findById(10L)).willReturn(Optional.of(room));
            given(userRepository.findById(1L)).willReturn(Optional.of(owner));

            PayRoomResponse response = payRoomService.changeRoundUnit(10L, 1L, 1000);

            assertThat(response.getRoundUnit()).isEqualTo(1000);
            assertThat(room.getTitle()).isEqualTo("Podo MT");
            assertThat(room.getDueDate()).isEqualTo(LocalDate.of(2026, 6, 12));
        }

        @Test
        @DisplayName("방장이 아니면 절사 단위를 바꿀 수 없다")
        void changeRoundUnit_byNonOwner_throwsException() {
            given(payRoomRepository.findById(10L)).willReturn(Optional.of(payRoom));
            given(userRepository.findById(2L)).willReturn(Optional.of(otherUser));

            assertThatThrownBy(() -> payRoomService.changeRoundUnit(10L, 2L, 1000))
                    .isInstanceOf(NotRoomOwnerException.class);

            assertThat(payRoom.getRoundUnit()).isEqualTo(10);   // 그대로
        }

        @Test
        @DisplayName("허용되지 않은 절사 단위면 예외가 나고 값은 바뀌지 않는다")
        void changeRoundUnit_invalidUnit_throwsException() {
            given(payRoomRepository.findById(10L)).willReturn(Optional.of(payRoom));
            given(userRepository.findById(1L)).willReturn(Optional.of(owner));

            assertThatThrownBy(() -> payRoomService.changeRoundUnit(10L, 1L, 50))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThat(payRoom.getRoundUnit()).isEqualTo(10);
        }
    }

    @Nested
    @DisplayName("멤버 추가 테스트")
    class AddMember {

        @Test
        @DisplayName("방장은 새 멤버를 추가할 수 있다")
        void addMember_byOwner_success() {
            given(payRoomRepository.findById(10L)).willReturn(Optional.of(payRoom));
            given(userRepository.findById(1L)).willReturn(Optional.of(owner));
            given(userRepository.findByLoginId("minwook")).willReturn(Optional.of(otherUser));
            given(roomMemberRepository.existsByPayRoomAndUser(payRoom, otherUser))
                    .willReturn(false);

            payRoomService.addMember(10L, 1L, "minwook");

            verify(roomMemberRepository).save(any(RoomMember.class));
        }

        @Test
        @DisplayName("방장이 아니면 멤버를 추가할 수 없다")
        void addMember_byNonOwner_throwsException() {
            given(payRoomRepository.findById(10L)).willReturn(Optional.of(payRoom));
            given(userRepository.findById(2L)).willReturn(Optional.of(otherUser));

            assertThatThrownBy(() -> payRoomService.addMember(10L, 2L, "pien"))
                    .isInstanceOf(NotRoomOwnerException.class);

            verify(roomMemberRepository, never()).save(any(RoomMember.class));
        }

        @Test
        @DisplayName("이미 멤버인 사용자를 추가하려 하면 AlreadyRoomMemberException")
        void addMember_alreadyMember_throwsException() {
            given(payRoomRepository.findById(10L)).willReturn(Optional.of(payRoom));
            given(userRepository.findById(1L)).willReturn(Optional.of(owner));
            given(userRepository.findByLoginId("minwook")).willReturn(Optional.of(otherUser));
            given(roomMemberRepository.existsByPayRoomAndUser(payRoom, otherUser))
                    .willReturn(true);

            assertThatThrownBy(() -> payRoomService.addMember(10L, 1L, "minwook"))
                    .isInstanceOf(AlreadyRoomMemberException.class);

            verify(roomMemberRepository, never()).save(any(RoomMember.class));
        }
    }

    private User createUserWithId(Long id, User template) {
        try {
            Field idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(template, id);
            return template;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private PayRoom createPayRoomWithId(Long id, PayRoom template) {
        try {
            Field idField = PayRoom.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(template, id);

            return template;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setCreatedAt(PayRoom payRoom, LocalDateTime createdAt) {
        try {
            Field field = payRoom.getClass().getSuperclass().getDeclaredField("createdAt");
            field.setAccessible(true);
            field.set(payRoom, createdAt);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
}