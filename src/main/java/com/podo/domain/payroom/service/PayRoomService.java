package com.podo.domain.payroom.service;

import com.podo.domain.payroom.dto.response.AdminPayRoomDetailResponse;
import com.podo.domain.payroom.dto.response.AdminPayRoomListResponse;
import com.podo.domain.payroom.dto.response.MemberInfo;
import com.podo.domain.payroom.dto.request.PayRoomCreateRequest;
import com.podo.domain.payroom.dto.response.PayRoomResponse;
import com.podo.domain.payroom.dto.request.PayRoomUpdateRequest;
import com.podo.domain.payroom.entity.*;
import com.podo.domain.payroom.exception.AlreadyRoomMemberException;
import com.podo.domain.payroom.exception.NotRoomOwnerException;
import com.podo.domain.payroom.exception.PayRoomNotFoundException;
import com.podo.domain.payroom.repository.AccountRepository;
import com.podo.domain.payroom.repository.PayRoomRepository;
import com.podo.domain.payroom.repository.PaymentRepository;
import com.podo.domain.payroom.repository.RoomMemberRepository;
import com.podo.domain.user.entity.User;
import com.podo.domain.user.exception.UserNotFoundException;
import com.podo.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PayRoomService {

    private final PayRoomRepository payRoomRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;
    private final AccountRepository accountRepository;

    @Transactional
    public PayRoomResponse create(Long ownerId, PayRoomCreateRequest request) {
        User owner = findUserById(ownerId);

        PayRoom payRoom = request.toEntity(owner);
        PayRoom savedRoom = payRoomRepository.save(payRoom);

        RoomMember ownerMember = RoomMember.builder()
                .payRoom(savedRoom)
                .user(owner)
                .build();
        roomMemberRepository.save(ownerMember);

        return PayRoomResponse.from(savedRoom);
    }

    public PayRoomResponse findById(Long roomId) {
        PayRoom payRoom = findPayRoomById(roomId);
        return PayRoomResponse.from(payRoom);
    }

    public List<MemberInfo> findMembers(Long roomId) {
        PayRoom payRoom = findPayRoomById(roomId);
        return roomMemberRepository.findByPayRoom(payRoom).stream()
                .map(rm -> MemberInfo.from(rm, payRoom))
                .toList();
    }

    public List<PayRoomResponse> findMyRooms(Long userId) {
        User user = findUserById(userId);
        return roomMemberRepository.findByUser(user).stream()
                .map(RoomMember::getPayRoom)
                .sorted(Comparator.comparing(PayRoom::getCreatedAt).reversed())
                .map(PayRoomResponse::from)
                .toList();
    }

    public List<PayRoomResponse> findMyRoomsByStatus(Long ownerId, RoomStatus status) {
        User owner = findUserById(ownerId);
        return payRoomRepository.findByOwnerAndStatus(owner, status).stream()
                .map(PayRoomResponse::from)
                .toList();
    }


    @Transactional
    public PayRoomResponse update(Long roomId, Long userId, PayRoomUpdateRequest request) {
        PayRoom payRoom = findPayRoomById(roomId);
        User user = findUserById(userId);

        validateOwner(payRoom, user);

        payRoom.updateInfo(request.getTitle(), request.getDueDate(), request.getRoundUnit());

        return PayRoomResponse.from(payRoom);
    }

    @Transactional
    public PayRoomResponse complete(Long roomId, Long userId) {
        PayRoom payRoom = findPayRoomById(roomId);
        User user = findUserById(userId);

        validateOwner(payRoom, user);

        payRoom.complete();

        return PayRoomResponse.from(payRoom);
    }

    @Transactional
    public PayRoomResponse reopen(Long roomId, Long userId) {
        PayRoom payRoom = findPayRoomById(roomId);
        User user = findUserById(userId);

        validateOwner(payRoom, user);

        payRoom.reopen();

        return PayRoomResponse.from(payRoom);
    }

    @Transactional
    public void addMember(Long roomId, Long ownerId, String newMemberLoginId) {
        PayRoom payRoom = findPayRoomById(roomId);
        User owner = findUserById(ownerId);

        validateOwner(payRoom, owner);

        User newMember = userRepository.findByLoginId(newMemberLoginId)
                .orElseThrow(() -> new UserNotFoundException(newMemberLoginId));

        if (roomMemberRepository.existsByPayRoomAndUser(payRoom, newMember)) {
            throw new AlreadyRoomMemberException();
        }

        RoomMember member = RoomMember.builder()
                .payRoom(payRoom)
                .user(newMember)
                .build();
        roomMemberRepository.save(member);
    }

    public Page<AdminPayRoomListResponse> findAllForAdmin(RoomStatus status, String keyword, Pageable pageable) {
        return payRoomRepository.searchForAdmin(status, keyword, pageable)
                .map(payRoom -> AdminPayRoomListResponse.of(
                        payRoom, roomMemberRepository.countByPayRoom(payRoom)));
    }

    public AdminPayRoomDetailResponse getDetailForAdmin(Long roomId) {
        PayRoom payRoom = findPayRoomById(roomId);
        List<RoomMember> members = roomMemberRepository.findByPayRoom(payRoom);
        List<Payment> payments = paymentRepository.findByPayRoom(payRoom);
        List<Account> accounts = accountRepository.findByPayRoom(payRoom);
        return AdminPayRoomDetailResponse.of(payRoom, members, payments, accounts);
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    private PayRoom findPayRoomById(Long roomId) {
        return payRoomRepository.findById(roomId)
                .orElseThrow(() -> new PayRoomNotFoundException(roomId));
    }

    private void validateOwner(PayRoom payRoom, User user) {
        if (!payRoom.isOwner(user)) {
            throw new NotRoomOwnerException();
        }
    }

}
