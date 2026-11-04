package com.podo.domain.payroom.service;

import com.podo.domain.payroom.dto.request.PayRoomCreateRequest;
import com.podo.domain.payroom.dto.response.PayRoomResponse;
import com.podo.domain.payroom.dto.request.PayRoomUpdateRequest;
import com.podo.domain.payroom.entity.PayRoom;
import com.podo.domain.payroom.entity.RoomMember;
import com.podo.domain.payroom.entity.RoomStatus;
import com.podo.domain.payroom.exception.NotRoomOwnerException;
import com.podo.domain.payroom.repository.PayRoomRepository;
import com.podo.domain.payroom.repository.RoomMemberRepository;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class PayRoomServiceIntegrationTest {

    @Autowired
    private PayRoomService payRoomService;

    @Autowired
    private PayRoomRepository payRoomRepository;

    @Autowired
    private RoomMemberRepository roomMemberRepository;

    @Autowired
    private UserRepository userRepository;

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

    @Test
    @DisplayName("방 생성 시 방장이 자동으로 RoomMember에 추가된다 (진짜 DB)")
    void create_addOwnerAsMember_realDB() {
        PayRoomCreateRequest request = PayRoomCreateRequest.builder()
                .title("Podo MT")
                .roundUnit(10)
                .build();

        PayRoomResponse response = payRoomService.create(owner.getId(), request);

        PayRoom savedRoom = payRoomRepository.findById(response.getId()).orElseThrow();
        assertThat(savedRoom.getTitle()).isEqualTo("Podo MT");
        assertThat(savedRoom.getOwner().getId()).isEqualTo(owner.getId());
    }

    @Test
    @DisplayName("방 수정 시 JPA 변경 감지로 DB에 자동 반영된다")
    void update_dirtyChecking() {
        PayRoomResponse created = payRoomService.create(owner.getId(),
                PayRoomCreateRequest.builder().title("원래 제목").build());

        PayRoomUpdateRequest updateRequest = PayRoomUpdateRequest.builder()
                .title("수정된 제목")
                .roundUnit(100)
                .build();

        payRoomService.update(created.getId(), owner.getId(), updateRequest);

        PayRoom updated = payRoomRepository.findById(created.getId()).orElseThrow();
        assertThat(updated.getTitle()).isEqualTo("수정된 제목");
        assertThat(updated.getRoundUnit()).isEqualTo(100);
    }

    @Test
    @DisplayName("방장이 아닌 사용자가 방을 수정하려 하면 DB 변경 없이 예외 발생")
    void update_byNonOwner_noChange() {
        PayRoomResponse created = payRoomService.create(owner.getId(),
                PayRoomCreateRequest.builder().title("원래 제목").build());

        PayRoomUpdateRequest updateRequest = PayRoomUpdateRequest.builder()
                .title("바꿀려고 했지만...")
                .build();

        assertThatThrownBy(() -> payRoomService.update(created.getId(), otherUser.getId(), updateRequest))
                .isInstanceOf(NotRoomOwnerException.class);

        PayRoom unchanged = payRoomRepository.findById(created.getId()).orElseThrow();
        assertThat(unchanged.getTitle()).isEqualTo("원래 제목");
    }

    @Test
    @DisplayName("멤버 추가가 진짜 DB에 반영된다")
    void addMember_realDB() {
        PayRoomResponse created = payRoomService.create(owner.getId(),
                PayRoomCreateRequest.builder().title("Podo MT").build());

        payRoomService.addMember(created.getId(), owner.getId(), otherUser.getLoginId());

        PayRoom payRoom = payRoomRepository.findById(created.getId()).orElseThrow();
        List<RoomMember> members = roomMemberRepository.findByPayRoom(payRoom);
        assertThat(members).hasSize(2);
        assertThat(members).extracting(rm -> rm.getUser().getId())
                .containsExactlyInAnyOrder(owner.getId(), otherUser.getId());
    }

    @Test
    @DisplayName("방 완료 → 재개 흐름이 DB에 정상 반영된다")
    void completeAndReopen() {
        PayRoomResponse created = payRoomService.create(owner.getId(),
                PayRoomCreateRequest.builder().title("Podo MT").build());

        payRoomService.complete(created.getId(), owner.getId());

        PayRoom completed = payRoomRepository.findById(created.getId()).orElseThrow();
        assertThat(completed.getStatus()).isEqualTo(RoomStatus.COMPLETED);

        payRoomService.reopen(created.getId(), owner.getId());

        PayRoom reopened = payRoomRepository.findById(created.getId()).orElseThrow();
        assertThat(reopened.getStatus()).isEqualTo(RoomStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("내 방 목록 조회 - 다른 사람의 방은 보이지 않는다")
    void findMyRooms_onlyMine() {
        payRoomService.create(owner.getId(),
                PayRoomCreateRequest.builder().title("내 방 1").build());
        payRoomService.create(owner.getId(),
                PayRoomCreateRequest.builder().title("내 방 2").build());
        payRoomService.create(otherUser.getId(),
                PayRoomCreateRequest.builder().title("남의 방").build());

        List<PayRoomResponse> myRooms = payRoomService.findMyRooms(owner.getId());

        assertThat(myRooms).hasSize(2);
        assertThat(myRooms).extracting(PayRoomResponse::getTitle)
                .containsExactlyInAnyOrder("내 방 1", "내 방 2");
    }
}
