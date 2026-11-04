package com.podo.domain.user.repository;

import com.podo.domain.user.entity.Role;
import com.podo.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByLoginId(String loginId);

    Optional<User> findByEmail(String email);

    boolean existsByLoginId(String loginId);

    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u " +
            "WHERE (u.loginId LIKE %:keyword% OR u.name LIKE  %:keyword%) " +
            "AND u.deleted = false")
    List<User> searchByKeyword(@Param("keyword") String keyword);

    @Query("SELECT u FROM User u " +
            "WHERE u.role = :role " +
            "AND (:deleted IS NULL OR u.deleted = :deleted) " +
            "AND (u.name LIKE CONCAT('%', :keyword, '%') " +
            "  OR u.loginId LIKE CONCAT('%', :keyword, '%') " +
            "  OR u.email LIKE CONCAT('%', :keyword, '%'))")
    Page<User> searchForAdmin(@Param("role") Role role,
                              @Param("deleted") Boolean deleted,
                              @Param("keyword") String keyword,
                              Pageable pageable);
}
