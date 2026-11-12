package com.podo.domain.payroom.service;

import com.podo.domain.payroom.dto.request.AccountCreateRequest;
import com.podo.domain.payroom.dto.request.AccountUpdateRequest;
import com.podo.domain.payroom.dto.response.AccountResponse;
import com.podo.domain.payroom.entity.Account;
import com.podo.domain.payroom.entity.PayRoom;
import com.podo.domain.payroom.exception.*;
import com.podo.domain.payroom.repository.AccountRepository;
import com.podo.domain.payroom.repository.PayRoomRepository;
import com.podo.domain.payroom.repository.RoomMemberRepository;
import com.podo.domain.user.entity.User;
import com.podo.domain.user.exception.UserNotFoundException;
import com.podo.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountService {

    private final AccountRepository accountRepository;
    private final PayRoomRepository payRoomRepository;
    private final UserRepository userRepository;
    private final RoomMemberRepository roomMemberRepository;

    @Transactional
    public AccountResponse create(Long roomId, Long currentUserId, AccountCreateRequest request) {
        PayRoom payRoom = findPayRoomById(roomId);
        User currentUser = findUserById(currentUserId);
        validateRoomMember(payRoom, currentUser);

        User accountOwner = findUserById(request.getUserId());
        validateRoomMember(payRoom, accountOwner);
        validateAccountModifier(payRoom, currentUser, accountOwner);

        Account account = accountRepository.save(Account.builder()
                .payRoom(payRoom)
                .user(accountOwner)
                .bank(request.getBank())
                .accountNumber(request.getAccountNumber())
                .holderName(accountOwner.getName())
                .build());

        return AccountResponse.from(account);
    }

    // 계좌번호는 민감 정보라 방 멤버만 조회 가능
    public List<AccountResponse> findByRoom(Long roomId, Long currentUserId) {
        PayRoom payRoom = findPayRoomById(roomId);
        if (!roomMemberRepository.existsMember(roomId, currentUserId)) {
            throw new NotRoomMemberException();
        }

        return accountRepository.findAllByPayRoomWithUser(payRoom).stream()
                .map(AccountResponse::from)
                .toList();

//        validateRoomMember(payRoom, findUserById(currentUserId));
//        return accountRepository.findByPayRoom(payRoom).stream()
//                .map(AccountResponse::from)
//                .toList();
    }

    @Transactional
    public AccountResponse update(Long roomId, Long accountId, Long currentUserId, AccountUpdateRequest request) {
        Account account = findAccountInRoom(roomId, accountId);
        User currentUser = findUserById(currentUserId);
        validateAccountModifier(account.getPayRoom(), currentUser, account.getUser());

        // 예금주는 입력받지 않고 계좌 소유자 이름으로 유지
        account.update(request.getBank(), request.getAccountNumber(), account.getUser().getName());
        return AccountResponse.from(account);
    }

    @Transactional
    public void delete(Long roomId, Long accountId, Long currentUserId) {
        Account account = findAccountInRoom(roomId, accountId);
        User currentUser = findUserById(currentUserId);
        validateAccountModifier(account.getPayRoom(), currentUser, account.getUser());

        accountRepository.delete(account);
    }

    private PayRoom findPayRoomById(Long roomId) {
        return payRoomRepository.findById(roomId)
                .orElseThrow(() -> new PayRoomNotFoundException(roomId));
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    private Account findAccountById(Long accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
    }

    // URL의 방과 계좌가 속한 방이 다르면 "없음"으로 응답 (존재 여부를 알려 주지 않음)
    private Account findAccountInRoom(Long roomId, Long accountId) {
        Account account = findAccountById(accountId);
        if (!account.getPayRoom().getId().equals(roomId)) {
            throw new AccountNotFoundException(accountId);
        }
        return account;
    }

    private void validateAccountModifier(PayRoom payRoom, User currentUser, User accountOwner) {
        boolean isOwner = payRoom.isOwner(currentUser);
        boolean isSelf = currentUser.getId().equals(accountOwner.getId());
        if (!isOwner && !isSelf) {
            throw new NotAccountModifierException();
        }
    }

    private void validateRoomMember(PayRoom payRoom, User user) {
        if (!roomMemberRepository.existsByPayRoomAndUser(payRoom, user)) {
            throw new NotRoomMemberException();
        }
    }
}