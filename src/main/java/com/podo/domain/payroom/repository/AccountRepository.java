package com.podo.domain.payroom.repository;

import com.podo.domain.payroom.entity.Account;
import com.podo.domain.payroom.entity.PayRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccountRepository extends JpaRepository<Account, Long> {

    List<Account> findByPayRoom(PayRoom payRoom);

}
