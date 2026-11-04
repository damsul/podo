package com.podo.domain.cs.repository;

import com.podo.domain.cs.entity.Inquiry;
import com.podo.domain.cs.entity.InquiryStatus;
import com.podo.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {
    @Query("SELECT i FROM Inquiry i WHERE i.user = :user " +
            "AND (:keyword IS NULL OR i.title LIKE %:keyword%) " +
            "ORDER BY i.id DESC")
    Page<Inquiry> findByUser(@Param("user") User user, @Param("keyword") String keyword, Pageable pageable);

    @Query(value = "SELECT i FROM Inquiry i JOIN FETCH i.user " +
            "WHERE (:status IS NULL OR i.status = :status) " +
            "AND (:keyword IS NULL OR i.title LIKE %:keyword%) " +
            "ORDER BY i.id DESC",
            countQuery = "SELECT COUNT(i) FROM Inquiry i " +
                    "WHERE (:status IS NULL OR i.status = :ststus) " +
                    "AND (:keyword IS NULL OR i.title LIKE %:keyword%)")
    Page<Inquiry> searchForAdmin(@Param("status") InquiryStatus status,
                                 @Param("keyword") String keyword,
                                 Pageable pageable);
}

