package com.podo.domain.cs.repository;

import com.podo.domain.cs.entity.Notice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

    @Query("SELECT n FROM Notice n WHERE :keyword IS NULL OR n.title LIKE %:keyword% " +
            "ORDER BY n.pinned DESC, n.createdAt DESC")
    Page<Notice> search(@Param("keyword") String keyword, Pageable pageable);
}
