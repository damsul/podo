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

    public List<AccountResponse> findByRoom(Long roomId) {
        PayRoom payRoom = findPayRoomById(roomId);
        return accountRepository.findByPayRoom(payRoom).stream()
                .map(AccountResponse::from)
                .toList();
    }

    @Transactional
    public AccountResponse update(Long accountId, Long currentUserId, AccountUpdateRequest request) {
        Account account = findAccountById(accountId);
        User currentUser = findUserById(currentUserId);
        validateAccountModifier(account.getPayRoom(), currentUser, account.getUser());

        account.update(request.getBank(), request.getAccountNumber(), request.getHolderName());
        return AccountResponse.from(account);
    }

    @Transactional
    public void delete(Long accountId, Long currentUserId) {
        Account account = findAccountById(accountId);
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
